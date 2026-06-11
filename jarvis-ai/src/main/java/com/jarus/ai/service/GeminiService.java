package com.jarus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jarus.ai.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiService {

    // 30-minute in-memory cache — avoids redundant Gemini calls and free-tier 429s
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private record CachedResponse(String response, long timestamp) {
        boolean isValid() { return System.currentTimeMillis() - timestamp < CACHE_TTL_MS; }
    }
    private final ConcurrentHashMap<Integer, CachedResponse> responseCache = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("geminiWebClient")
    private WebClient geminiWebClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // Fallback models tried in order when primary is rate-limited
    private static final List<String> FALLBACK_MODELS = List.of(
        "gemini-1.5-flash", "gemini-1.5-flash-8b", "gemini-1.5-pro"
    );

    public String chat(String message, String apiKey) {
        return callGemini(message, apiKey);
    }

    /**
     * Verifies an API key by making a minimal Gemini call.
     * Returns: "VERIFIED", "RATE_LIMITED", "INVALID_KEY", or "NETWORK_ERROR".
     */
    public String verifyKey(String apiKey) {
        try {
            callGemini("Say OK", apiKey);
            return "VERIFIED";
        } catch (com.jarus.ai.exception.GeminiRateLimitException e) {
            return "RATE_LIMITED";
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401 || status == 403) return "INVALID_KEY";
            return "NETWORK_ERROR";
        } catch (Exception e) {
            return "NETWORK_ERROR";
        }
    }

    public TailoredResume tailorResume(ParsedResume resume, JobPost job, String apiKey) {
        String prompt = buildTailorPrompt(resume, job);
        String responseText = callGemini(prompt, apiKey);
        return parseTailoredResume(resume, job, responseText);
    }

    public CompanyResearch researchCompany(String company, String jobTitle, String jd, String apiKey) {
        String prompt = "Research the company \"" + company + "\" for a candidate applying to the role of \"" + jobTitle + "\".\n"
                + "Job description excerpt: " + (jd != null ? jd.substring(0, Math.min(500, jd.length())) : "") + "\n\n"
                + "Respond in JSON:\n"
                + "{\n"
                + "  \"overview\": \"2-3 sentence company overview\",\n"
                + "  \"interviewRounds\": <number>,\n"
                + "  \"interviewQuestions\": [\"question1\", \"question2\", ...up to 10],\n"
                + "  \"tips\": \"key tips for the interview\"\n"
                + "}";
        String raw = callGemini(prompt, apiKey);
        return parseCompanyResearch(company, raw);
    }

    public JobMatchResult analyzeJobMatch(ParsedResume resume, JobPost job, String apiKey) {
        String prompt = "Analyze how well this resume matches the job.\n\n"
                + "RESUME TEXT:\n" + truncate(resume.getFullText(), 2000) + "\n\n"
                + "JOB TITLE: " + job.getTitle() + "\n"
                + "JOB DESCRIPTION:\n" + truncate(job.getDescription(), 1500) + "\n\n"
                + "Respond in JSON:\n"
                + "{\n"
                + "  \"matchScore\": <0-100>,\n"
                + "  \"matchedSkills\": [\"skill1\", \"skill2\"],\n"
                + "  \"missingSkills\": [\"skill1\", \"skill2\"],\n"
                + "  \"recommendation\": \"brief recommendation\"\n"
                + "}";
        String raw = callGemini(prompt, apiKey);
        return parseJobMatch(raw);
    }

    public String generateCoverLetter(ParsedResume resume, JobPost job, String apiKey) {
        String prompt = "Write a professional cover letter for the following job application.\n\n"
                + "CANDIDATE RESUME:\n" + truncate(resume.getFullText(), 1500) + "\n\n"
                + "JOB TITLE: " + job.getTitle() + " at " + job.getCompany() + "\n"
                + "JOB DESCRIPTION:\n" + truncate(job.getDescription(), 1000) + "\n\n"
                + "Write a compelling, concise cover letter (300-400 words). "
                + "Highlight relevant skills and express genuine interest. "
                + "Return ONLY the cover letter text, no extra commentary.";
        return callGemini(prompt, apiKey);
    }

    private String callGemini(String prompt, String apiKey) {
        // Check cache first (keyed by prompt content — same prompt → same answer)
        int cacheKey = prompt.hashCode();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && cached.isValid()) return cached.response();

        // Try primary model first, then fallbacks on 429
        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(geminiModel);
        modelsToTry.addAll(FALLBACK_MODELS.stream()
            .filter(m -> !m.equals(geminiModel)).toList());

        WebClientResponseException lastEx = null;
        for (String model : modelsToTry) {
            try {
                String result = callGeminiModel(prompt, apiKey, model);
                responseCache.put(cacheKey, new CachedResponse(result, System.currentTimeMillis()));
                return result;
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 429) {
                    lastEx = e;
                    // try next model
                } else {
                    throw new RuntimeException("Gemini API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
                }
            }
        }
        // All models rate-limited
        throw new com.jarus.ai.exception.GeminiRateLimitException(
            "Gemini rate limit exceeded on all models — please wait a minute and retry");
    }

    private String callGeminiModel(String prompt, String apiKey, String model) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode genConfig = requestBody.putObject("generationConfig");
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 4096);

        String responseBody;
        responseBody = geminiWebClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String result = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + responseBody, e);
        }
    }

    private String buildTailorPrompt(ParsedResume resume, JobPost job) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a resume tailoring assistant. Tailor ONLY relevant sections of this resume to match the job.\n\n");
        sb.append("JOB TITLE: ").append(job.getTitle()).append("\n");
        sb.append("COMPANY: ").append(job.getCompany()).append("\n");
        sb.append("JOB DESCRIPTION:\n").append(truncate(job.getDescription(), 1500)).append("\n\n");
        sb.append("CURRENT RESUME SECTIONS:\n");
        if (resume.getSections() != null) {
            for (ResumeSection sec : resume.getSections()) {
                sb.append("[").append(sec.getName()).append("]\n").append(sec.getOriginalContent()).append("\n\n");
            }
        } else {
            sb.append(truncate(resume.getFullText(), 2000)).append("\n");
        }
        sb.append("\nRespond in JSON with ONLY sections that need modification:\n");
        sb.append("{\n  \"modifiedSections\": [\n");
        sb.append("    {\"name\": \"SECTION_NAME\", \"modifiedContent\": \"new content\", \"changeReason\": \"why changed\"}\n");
        sb.append("  ],\n  \"changesSummary\": [\"change 1\", \"change 2\"]\n}\n");
        sb.append("Do NOT modify sections that are already a good match. Preserve the candidate's facts — only rephrase/reorder.");
        return sb.toString();
    }

    private TailoredResume parseTailoredResume(ParsedResume original, JobPost job, String raw) {
        TailoredResume t = new TailoredResume();
        t.setOriginalResumeId(original.getId());
        t.setJobId(job.getId());
        t.setCreatedAt(System.currentTimeMillis());

        // Copy all original sections
        List<ResumeSection> allSections = new ArrayList<>();
        if (original.getSections() != null) {
            for (ResumeSection s : original.getSections()) {
                ResumeSection copy = new ResumeSection();
                copy.setName(s.getName());
                copy.setOriginalContent(s.getOriginalContent());
                copy.setModifiedContent(s.getOriginalContent());
                copy.setWasModified(false);
                allSections.add(copy);
            }
        }

        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            JsonNode modified = root.path("modifiedSections");
            if (modified.isArray()) {
                for (JsonNode node : modified) {
                    String sectionName = node.path("name").asText();
                    String modifiedContent = node.path("modifiedContent").asText();
                    String reason = node.path("changeReason").asText();
                    for (ResumeSection sec : allSections) {
                        if (sec.getName().equalsIgnoreCase(sectionName)) {
                            sec.setModifiedContent(modifiedContent);
                            sec.setWasModified(true);
                            sec.setChangeReason(reason);
                            break;
                        }
                    }
                }
            }
            JsonNode summary = root.path("changesSummary");
            List<String> summaryList = new ArrayList<>();
            if (summary.isArray()) {
                for (JsonNode s : summary) summaryList.add(s.asText());
            }
            t.setChangesSummary(summaryList);
        } catch (Exception e) {
            t.setChangesSummary(List.of("Resume tailored by AI"));
        }
        t.setModifiedSections(allSections);
        return t;
    }

    private CompanyResearch parseCompanyResearch(String company, String raw) {
        CompanyResearch cr = new CompanyResearch();
        cr.setCompanyName(company);
        cr.setLinkedInSearchUrl("https://www.linkedin.com/search/results/companies/?keywords=" + encodeUri(company));
        cr.setGlassdoorSearchUrl("https://www.glassdoor.com/Search/results.htm?keyword=" + encodeUri(company));
        cr.setGoogleSearchUrl("https://www.google.com/search?q=" + encodeUri(company + " company interview"));
        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            cr.setOverview(root.path("overview").asText());
            cr.setInterviewRounds(root.path("interviewRounds").asInt(3));
            cr.setTips(root.path("tips").asText());
            List<String> questions = new ArrayList<>();
            for (JsonNode q : root.path("interviewQuestions")) questions.add(q.asText());
            cr.setInterviewQuestions(questions);
        } catch (Exception e) {
            cr.setOverview(raw);
        }
        return cr;
    }

    private JobMatchResult parseJobMatch(String raw) {
        JobMatchResult result = new JobMatchResult();
        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            result.setMatchScore(root.path("matchScore").asInt(50));
            result.setRecommendation(root.path("recommendation").asText());
            List<String> matched = new ArrayList<>();
            for (JsonNode s : root.path("matchedSkills")) matched.add(s.asText());
            result.setMatchedSkills(matched);
            List<String> missing = new ArrayList<>();
            for (JsonNode s : root.path("missingSkills")) missing.add(s.asText());
            result.setMissingSkills(missing);
        } catch (Exception e) {
            result.setMatchScore(0);
            result.setRecommendation("Could not analyze match");
        }
        return result;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String encodeUri(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
