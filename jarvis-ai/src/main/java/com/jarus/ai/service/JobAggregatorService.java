package com.jarus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.JobPost;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public List<JobPost> fetchJobs(String keywords, String location, String userId) {
        List<JobPost> allJobs = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        // Indeed RSS
        try {
            allJobs.addAll(fetchIndeedRss(keywords, location, userId, seenUrls));
        } catch (Exception e) {
            // Source failure is non-fatal
        }

        // RemoteOK (remote jobs)
        try {
            allJobs.addAll(fetchRemoteOk(keywords, userId, seenUrls));
        } catch (Exception e) {
            // Source failure is non-fatal
        }

        // Arbeitnow (international)
        try {
            allJobs.addAll(fetchArbeitnow(keywords, userId, seenUrls));
        } catch (Exception e) {
            // Source failure is non-fatal
        }

        return allJobs;
    }

    private List<JobPost> fetchIndeedRss(String keywords, String location, String userId, Set<String> seenUrls) throws Exception {
        String url = "https://www.indeed.com/rss?q=" + encode(keywords != null ? keywords : "software engineer")
                + "&l=" + encode(location != null ? location : "");
        byte[] rssBytes = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        if (rssBytes == null) return Collections.emptyList();

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(rssBytes)));
        List<JobPost> jobs = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            String link = entry.getLink();
            if (link == null || seenUrls.contains(link)) continue;
            seenUrls.add(link);
            JobPost job = new JobPost();
            job.setId(UUID.randomUUID().toString());
            job.setUserId(userId);
            job.setTitle(entry.getTitle());
            job.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : "");
            job.setUrl(link);
            job.setSource("Indeed");
            job.setStatus("NEW");
            job.setNewToday(true);
            job.setCapturedAt(entry.getPublishedDate() != null ? entry.getPublishedDate().getTime() : System.currentTimeMillis());
            jobs.add(job);
        }
        return jobs;
    }

    private List<JobPost> fetchRemoteOk(String keywords, String userId, Set<String> seenUrls) throws Exception {
        String response = webClient.get()
                .uri("https://remoteok.com/api")
                .header("User-Agent", "JARUS-Job-Agent/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        String kw = keywords != null ? keywords.toLowerCase() : "";
        for (JsonNode node : root) {
            if (!node.has("url")) continue;
            String title = node.path("position").asText();
            if (!kw.isEmpty() && !title.toLowerCase().contains(kw) &&
                    !node.path("tags").toString().toLowerCase().contains(kw)) continue;
            String url = node.path("url").asText();
            if (seenUrls.contains(url)) continue;
            seenUrls.add(url);
            JobPost job = new JobPost();
            job.setId(UUID.randomUUID().toString());
            job.setUserId(userId);
            job.setTitle(title);
            job.setCompany(node.path("company").asText());
            job.setDescription(node.path("description").asText());
            job.setUrl(url);
            job.setSource("RemoteOK");
            job.setStatus("NEW");
            job.setNewToday(true);
            job.setCapturedAt(System.currentTimeMillis());
            jobs.add(job);
        }
        return jobs;
    }

    private List<JobPost> fetchArbeitnow(String keywords, String userId, Set<String> seenUrls) throws Exception {
        String response = webClient.get()
                .uri("https://arbeitnow.com/api/job-board-api")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (response == null) return Collections.emptyList();
        JsonNode root = objectMapper.readTree(response);
        List<JobPost> jobs = new ArrayList<>();
        String kw = keywords != null ? keywords.toLowerCase() : "";
        for (JsonNode node : root.path("data")) {
            String title = node.path("title").asText();
            if (!kw.isEmpty() && !title.toLowerCase().contains(kw)) continue;
            String url = node.path("url").asText();
            if (seenUrls.contains(url)) continue;
            seenUrls.add(url);
            JobPost job = new JobPost();
            job.setId(UUID.randomUUID().toString());
            job.setUserId(userId);
            job.setTitle(title);
            job.setCompany(node.path("company_name").asText());
            job.setDescription(node.path("description").asText());
            job.setUrl(url);
            job.setSource("Arbeitnow");
            job.setStatus("NEW");
            job.setNewToday(true);
            job.setCapturedAt(System.currentTimeMillis());
            jobs.add(job);
        }
        return jobs;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
