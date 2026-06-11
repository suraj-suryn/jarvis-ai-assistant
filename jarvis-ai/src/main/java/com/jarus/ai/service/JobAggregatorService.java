package com.jarus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.JobPost;
import com.jarus.ai.model.UserSettings;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JobAggregatorService {

    @Autowired
    @Qualifier("generalWebClient")
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    // Adzuna (free key from developer.adzuna.com)
    @Value("${adzuna.app.id:}") private String adzunaAppId;
    @Value("${adzuna.app.key:}") private String adzunaAppKey;

    // Jooble (free key from api@jooble.org)
    @Value("${jooble.api.key:}") private String joobleApiKey;

    // Per-source enabled flags (admin can disable globally via env)
    @Value("${job.sources.remotive.enabled:true}")   private boolean remotiveEnabled;
    @Value("${job.sources.themuse.enabled:true}")     private boolean theMuseEnabled;
    @Value("${job.sources.greenhouse.enabled:true}")  private boolean greenhouseEnabled;
    @Value("${job.sources.lever.enabled:true}")       private boolean leverEnabled;
    @Value("${job.sources.remoteok.enabled:true}")    private boolean remoteOkEnabled;
    @Value("${job.sources.arbeitnow.enabled:true}")   private boolean arbeitnowEnabled;

    /** All known sources — shown in Settings UI */
    public static final List<String> ALL_SOURCES = List.of(
        "RemoteOK", "Remotive", "TheMuse", "Greenhouse", "Lever", "Arbeitnow", "Adzuna", "Jooble"
    );

    /**
     * Fetch jobs using the user's preferences:
     *  - keywords + location from UserSettings
     *  - jobType filter (ANY / REMOTE / HYBRID / ONSITE)
     *  - experienceLevel filter (ANY / ENTRY / MID / SENIOR / LEAD)
     *  - enabledSources list (empty = all enabled sources)
     */
    public List<JobPost> fetchJobs(String keywords, String location, String userId) {
        return fetchJobsWithPrefs(keywords, location, userId, null);
    }

    public List<JobPost> fetchJobsWithPrefs(String keywords, String location, String userId, UserSettings prefs) {
        List<JobPost> allJobs = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        String jobType = prefs != null && prefs.getJobType() != null ? prefs.getJobType() : "ANY";
        String expLevel = prefs != null && prefs.getExperienceLevel() != null ? prefs.getExperienceLevel() : "ANY";
        List<String> userSources = prefs != null && prefs.getEnabledSources() != null && !prefs.getEnabledSources().isEmpty()
                ? prefs.getEnabledSources() : ALL_SOURCES;

        if (remoteOkEnabled && userSources.contains("RemoteOK")) {
            try { allJobs.addAll(fetchRemoteOk(keywords, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (remotiveEnabled && userSources.contains("Remotive")) {
            try { allJobs.addAll(fetchRemotive(keywords, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (theMuseEnabled && userSources.contains("TheMuse")) {
            try { allJobs.addAll(fetchTheMuse(keywords, location, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (greenhouseEnabled && userSources.contains("Greenhouse")) {
            try { allJobs.addAll(fetchGreenhouse(keywords, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (leverEnabled && userSources.contains("Lever")) {
            try { allJobs.addAll(fetchLever(keywords, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (arbeitnowEnabled && userSources.contains("Arbeitnow")) {
            try { allJobs.addAll(fetchArbeitnow(keywords, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (!adzunaAppId.isBlank() && !adzunaAppKey.isBlank() && userSources.contains("Adzuna")) {
            try { allJobs.addAll(fetchAdzuna(keywords, location, userId, seenUrls)); } catch (Exception ignored) {}
        }
        if (!joobleApiKey.isBlank() && userSources.contains("Jooble")) {
            try { allJobs.addAll(fetchJooble(keywords, location, userId, seenUrls)); } catch (Exception ignored) {}
        }

        // Apply job type filter
        if (!"ANY".equalsIgnoreCase(jobType)) {
            allJobs.removeIf(j -> !matchesJobType(j, jobType));
        }

        // Apply experience level filter
        if (!"ANY".equalsIgnoreCase(expLevel)) {
            allJobs.removeIf(j -> !matchesExperience(j, expLevel));
        }

        return allJobs;
    }

    // ── Sources ────────────────────────────────────────────────────────────────

    private List<JobPost> fetchRemoteOk(String keywords, String userId, Set<String> seenUrls) throws Exception {
        String response = webClient.get()
                .uri("https://remoteok.com/api")
                .header("User-Agent", "JARUS-Job-Agent/1.0")
                .retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        String kw = lower(keywords);
        for (JsonNode node : root) {
            if (!node.has("url")) continue;
            String title = node.path("position").asText();
            if (!matchesKeywords(title + " " + node.path("tags").toString(), kw)) continue;
            String url = node.path("url").asText();
            if (!seenUrls.add(url)) continue;
            String desc = node.path("description").asText();
            // RemoteOK provides salary_min / salary_max in USD
            String salary = null;
            if (node.has("salary_min") && !node.path("salary_min").asText().isBlank() && !node.path("salary_min").asText().equals("0")) {
                String min = node.path("salary_min").asText();
                String max = node.path("salary_max").asText();
                salary = "$" + min + (max != null && !max.isBlank() && !max.equals("0") ? " – $" + max : "+") + " /yr";
            } else {
                salary = extractSalaryFromDesc(desc);
            }
            JobPost job = build(userId, title, node.path("company").asText(), desc, url, "RemoteOK", "REMOTE");
            if (salary != null) job.setSalary(salary);
            jobs.add(job);
        }
        return jobs;
    }

    private List<JobPost> fetchRemotive(String keywords, String userId, Set<String> seenUrls) throws Exception {
        String kw = encode(keywords != null ? keywords : "software");
        String response = webClient.get()
                .uri("https://remotive.com/api/remote-jobs?search=" + kw + "&limit=50")
                .retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        for (JsonNode node : root.path("jobs")) {
            String url = node.path("url").asText();
            if (!seenUrls.add(url)) continue;
            String desc = node.path("description").asText();
            // Remotive provides salary field directly
            String salary = null;
            String remotiveSalary = node.path("salary").asText("");
            if (!remotiveSalary.isBlank()) {
                salary = remotiveSalary;
            } else {
                salary = extractSalaryFromDesc(desc);
            }
            JobPost job = build(userId, node.path("title").asText(), node.path("company_name").asText(),
                    desc, url, "Remotive", "REMOTE");
            if (salary != null) job.setSalary(salary);
            jobs.add(job);
        }
        return jobs;
    }

    private List<JobPost> fetchTheMuse(String keywords, String location, String userId, Set<String> seenUrls) throws Exception {
        String kw = encode(keywords != null ? keywords : "software engineer");
        String response = webClient.get()
                .uri("https://www.themuse.com/api/public/jobs?descending=true&page=1&category=" + kw + "&level=&location=&company=")
                .header("User-Agent", "JARUS-Job-Agent/1.0")
                .retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        String kwLow = lower(keywords);
        for (JsonNode node : root.path("results")) {
            String title = node.path("name").asText();
            if (!matchesKeywords(title, kwLow)) continue;
            String url = node.path("refs").path("landing_page").asText();
            if (url.isBlank() || !seenUrls.add(url)) continue;
            String company = node.path("company").path("name").asText();
            String locStr = node.path("locations").isArray() && node.path("locations").size() > 0
                    ? node.path("locations").get(0).path("name").asText() : "";
            String wtype = locStr.toLowerCase().contains("remote") ? "REMOTE" : "ONSITE";
            jobs.add(build(userId, title, company, node.path("contents").asText(), url, "TheMuse", wtype));
        }
        return jobs;
    }

    private List<JobPost> fetchGreenhouse(String keywords, String userId, Set<String> seenUrls) throws Exception {
        // Well-known tech companies using Greenhouse ATS
        List<String> companies = List.of("stripe", "shopify", "airbnb", "dropbox", "zendesk",
                "hashicorp", "confluent", "figma", "notion", "linear", "vercel", "supabase",
                "postman", "atlassian", "hubspot", "brex", "plaid", "robinhood");
        List<JobPost> jobs = new ArrayList<>();
        String kwLow = lower(keywords);
        for (String co : companies) {
            try {
                String response = webClient.get()
                        .uri("https://boards-api.greenhouse.io/v1/boards/" + co + "/jobs?content=true")
                        .retrieve().bodyToMono(String.class).block();
                if (response == null) continue;
                JsonNode root = objectMapper.readTree(response);
                for (JsonNode node : root.path("jobs")) {
                    String title = node.path("title").asText();
                    if (!matchesKeywords(title, kwLow)) continue;
                    String url = node.path("absolute_url").asText();
                    if (url.isBlank() || !seenUrls.add(url)) continue;
                    jobs.add(build(userId, title, capitalize(co),
                            node.path("content").asText(), url, "Greenhouse", null));
                }
            } catch (Exception ignored) {}
        }
        return jobs;
    }

    private List<JobPost> fetchLever(String keywords, String userId, Set<String> seenUrls) throws Exception {
        // Well-known companies using Lever ATS
        List<String> companies = List.of("netflix", "reddit", "twilio", "sendgrid", "datadog",
                "segment", "intercom", "gusto", "carta", "benchling", "scale-ai", "asana");
        List<JobPost> jobs = new ArrayList<>();
        String kwLow = lower(keywords);
        for (String co : companies) {
            try {
                String response = webClient.get()
                        .uri("https://api.lever.co/v0/postings/" + co + "?mode=json&limit=50")
                        .retrieve().bodyToMono(String.class).block();
                if (response == null) continue;
                JsonNode root = objectMapper.readTree(response);
                for (JsonNode node : root) {
                    String title = node.path("text").asText();
                    if (!matchesKeywords(title, kwLow)) continue;
                    String url = node.path("hostedUrl").asText();
                    if (url.isBlank() || !seenUrls.add(url)) continue;
                    String loc = node.path("categories").path("location").asText();
                    String wtype = loc.toLowerCase().contains("remote") ? "REMOTE" : "ONSITE";
                    String desc = node.path("descriptionPlain").asText();
                    jobs.add(build(userId, title, capitalize(co), desc, url, "Lever", wtype));
                }
            } catch (Exception ignored) {}
        }
        return jobs;
    }

    private List<JobPost> fetchArbeitnow(String keywords, String userId, Set<String> seenUrls) throws Exception {
        String response = webClient.get()
                .uri("https://arbeitnow.com/api/job-board-api")
                .retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        String kwLow = lower(keywords);
        for (JsonNode node : root.path("data")) {
            String title = node.path("title").asText();
            if (!matchesKeywords(title, kwLow)) continue;
            String url = node.path("url").asText();
            if (!seenUrls.add(url)) continue;
            String wtype = node.path("remote").asBoolean() ? "REMOTE" : "ONSITE";
            String desc = node.path("description").asText();
            // Arbeitnow provides salary_range
            String salary = null;
            String arbSalary = node.path("salary_range").asText("");
            if (!arbSalary.isBlank()) {
                salary = arbSalary;
            } else {
                salary = extractSalaryFromDesc(desc);
            }
            JobPost job = build(userId, title, node.path("company_name").asText(),
                    desc, url, "Arbeitnow", wtype);
            if (salary != null) job.setSalary(salary);
            jobs.add(job);
        }
        return jobs;
    }

    private List<JobPost> fetchAdzuna(String keywords, String location, String userId, Set<String> seenUrls) throws Exception {
        String country = resolveAdzunaCountry(location);
        String url = "https://api.adzuna.com/v1/api/jobs/" + country + "/search/1"
                + "?app_id=" + adzunaAppId
                + "&app_key=" + adzunaAppKey
                + "&results_per_page=50"
                + "&what=" + encode(keywords != null ? keywords : "software engineer")
                + "&where=" + encode(location != null ? location : "")
                + "&content-type=application/json";
        String response = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        for (JsonNode node : root.path("results")) {
            String jobUrl = node.path("redirect_url").asText();
            if (jobUrl.isBlank() || !seenUrls.add(jobUrl)) continue;
            jobs.add(build(userId, node.path("title").asText(),
                    node.path("company").path("display_name").asText(),
                    node.path("description").asText(), jobUrl, "Adzuna", null));
        }
        return jobs;
    }

    private List<JobPost> fetchJooble(String keywords, String location, String userId, Set<String> seenUrls) throws Exception {
        String body = "{\"keywords\":\"" + (keywords != null ? keywords : "") + "\","
                + "\"location\":\"" + (location != null ? location : "") + "\"}";
        String response = webClient.post()
                .uri("https://jooble.org/api/" + joobleApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve().bodyToMono(String.class).block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        for (JsonNode node : root.path("jobs")) {
            String url = node.path("link").asText();
            if (url.isBlank() || !seenUrls.add(url)) continue;
            jobs.add(build(userId, node.path("title").asText(), node.path("company").asText(),
                    node.path("snippet").asText(), url, "Jooble", null));
        }
        return jobs;
    }

    // ── Filters ────────────────────────────────────────────────────────────────

    private boolean matchesKeywords(String text, String keywords) {
        if (keywords == null || keywords.isBlank()) return true;
        String t = text.toLowerCase();
        // Any single keyword token matches
        for (String kw : keywords.split("[,;\\s]+")) {
            if (!kw.isBlank() && t.contains(kw.trim())) return true;
        }
        return false;
    }

    private boolean matchesJobType(JobPost job, String jobType) {
        if (jobType == null || "ANY".equalsIgnoreCase(jobType)) return true;
        String wt = job.getWorkType() != null ? job.getWorkType().toUpperCase() : "";
        String desc = (job.getDescription() != null ? job.getDescription() : "").toLowerCase();
        String title = (job.getTitle() != null ? job.getTitle() : "").toLowerCase();
        return switch (jobType.toUpperCase()) {
            case "REMOTE"  -> wt.equals("REMOTE")  || desc.contains("remote")  || title.contains("remote");
            case "HYBRID"  -> wt.equals("HYBRID")  || desc.contains("hybrid")  || title.contains("hybrid");
            case "ONSITE"  -> wt.equals("ONSITE")  || (!desc.contains("remote") && !desc.contains("hybrid"));
            default -> true;
        };
    }

    private boolean matchesExperience(JobPost job, String level) {
        if (level == null || "ANY".equalsIgnoreCase(level)) return true;
        String combined = (job.getTitle() + " " + job.getDescription()).toLowerCase();
        return switch (level.toUpperCase()) {
            case "ENTRY"  -> combined.matches(".*\\b(junior|entry|graduate|fresher|trainee|0-1|1-2)\\b.*");
            case "MID"    -> combined.matches(".*\\b(mid|2-4|3-5|intermediate|associate)\\b.*");
            case "SENIOR" -> combined.matches(".*\\b(senior|sr\\.|5\\+|6\\+|7\\+|lead|principal)\\b.*");
            case "LEAD"   -> combined.matches(".*\\b(lead|staff|principal|architect|head of|manager)\\b.*");
            default -> true;
        };
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    // Extract salary range from free-text description using common patterns
    private String extractSalaryFromDesc(String desc) {
        if (desc == null || desc.isBlank()) return null;
        // Patterns: $120,000 – $180,000 | ₹15L – ₹25L | $120k-$180k | USD 120,000 | £60,000
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "(?i)(?:salary|compensation|pay|package|ctc|lpa|range)[^\\n]{0,30}?" +
            "([\\$\\£\\€\\u20b9][\\d,\\.]+[kKlLmM]?\\s*(?:[-–]\\s*[\\$\\£\\€\\u20b9][\\d,\\.]+[kKlLmM]?)?" +
            "(?:\\s*(?:per year|per annum|/yr|/year|pa|annually|lpa|ctc))?)"
        );
        java.util.regex.Matcher m = p.matcher(desc);
        if (m.find()) return m.group(1).trim();
        // Fallback: plain currency range like $120,000 – $180,000
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
            "([\\$\\£\\€\\u20b9][\\d,\\.]+[kKlLmM]?\\s*(?:[-–—]\\s*[\\$\\£\\€\\u20b9][\\d,\\.]+[kKlLmM]?))"
        );
        java.util.regex.Matcher m2 = p2.matcher(desc);
        if (m2.find()) return m2.group(1).trim();
        return null;
    }

    private JobPost build(String userId, String title, String company, String desc,
                           String url, String source, String workType) {
        JobPost job = new JobPost();
        job.setId(UUID.randomUUID().toString());
        job.setUserId(userId);
        job.setTitle(title);
        job.setCompany(company);
        job.setDescription(desc);
        job.setUrl(url);
        job.setSource(source);
        job.setWorkType(workType);
        job.setStatus("NEW");
        job.setNewToday(true);
        job.setCapturedAt(System.currentTimeMillis());
        return job;
    }

    private String resolveAdzunaCountry(String location) {
        if (location == null) return "in";
        String l = location.toLowerCase();
        if (l.contains("india") || l.contains("bangalore") || l.contains("mumbai") ||
            l.contains("hyderabad") || l.contains("pune") || l.contains("chennai") ||
            l.contains("delhi") || l.contains("remote")) return "in";
        if (l.contains("uk") || l.contains("london")) return "gb";
        if (l.contains("us") || l.contains("usa") || l.contains("new york") ||
            l.contains("san francisco") || l.contains("seattle")) return "us";
        if (l.contains("canada")) return "ca";
        if (l.contains("australia")) return "au";
        if (l.contains("germany")) return "de";
        return "in"; // default India
    }

    private String lower(String s) { return s != null ? s.toLowerCase() : ""; }
    private String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
