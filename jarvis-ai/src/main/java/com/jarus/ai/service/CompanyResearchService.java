package com.jarus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.CompanyResearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyResearchService {

    @Autowired
    @Qualifier("generalWebClient")
    private WebClient webClient;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${google.cse.api.key:}")
    private String cseApiKey;

    @Value("${google.cse.id:}")
    private String cseId;

    public CompanyResearch research(String company, String jobTitle, String jd, String geminiApiKey) {
        // Start with Gemini AI research
        CompanyResearch result = geminiService.researchCompany(company, jobTitle, jd, geminiApiKey);

        // Enrich with Wikipedia overview
        try {
            String wikiOverview = fetchWikipediaOverview(company);
            if (wikiOverview != null && !wikiOverview.isEmpty() &&
                    (result.getOverview() == null || result.getOverview().isEmpty())) {
                result.setOverview(wikiOverview);
            }
        } catch (Exception e) {
            // Wikipedia not critical
        }

        // Google CSE for additional links
        try {
            List<String> searchResults = fetchGoogleSearchResults(company + " " + jobTitle + " company");
            result.setSearchResults(searchResults);
        } catch (Exception e) {
            // CSE not critical
        }

        // Direct search links
        result.setLinkedInSearchUrl("https://www.linkedin.com/search/results/companies/?keywords=" + encode(company));
        result.setGlassdoorSearchUrl("https://www.glassdoor.com/Search/results.htm?keyword=" + encode(company));
        result.setGoogleSearchUrl("https://www.google.com/search?q=" + encode(company + " interview experience"));

        return result;
    }

    private String fetchWikipediaOverview(String company) {
        try {
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encode(company);
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode root = objectMapper.readTree(response);
            return root.path("extract").asText();
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fetchGoogleSearchResults(String query) {
        List<String> results = new ArrayList<>();
        if (cseApiKey.isEmpty() || cseId.isEmpty()) return results;
        try {
            String url = "https://www.googleapis.com/customsearch/v1?key=" + cseApiKey
                    + "&cx=" + cseId + "&q=" + encode(query) + "&num=5";
            String response = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
            JsonNode root = objectMapper.readTree(response);
            for (JsonNode item : root.path("items")) {
                results.add(item.path("title").asText() + ": " + item.path("link").asText());
            }
        } catch (Exception e) {
            // CSE failure is non-critical
        }
        return results;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
