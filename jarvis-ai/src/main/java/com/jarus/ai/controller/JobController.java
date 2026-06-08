package com.jarus.ai.controller;

import com.jarus.ai.model.JobPost;
import com.jarus.ai.model.JobMatchResult;
import com.jarus.ai.model.ParsedResume;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.JobRepository;
import com.jarus.ai.repository.ResumeRepository;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.security.EncryptionService;
import com.jarus.ai.service.GeminiService;
import com.jarus.ai.service.JobAggregatorService;
import com.jarus.ai.service.PushSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired private JobRepository jobRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GeminiService geminiService;
    @Autowired private JobAggregatorService aggregatorService;
    @Autowired private PushSubscriptionService pushService;
    @Autowired private EncryptionService encryptionService;

    @PostMapping("/capture")
    public ResponseEntity<?> capture(@RequestBody JobPost job, Authentication authentication) {
        String userId = getUserId(authentication);
        job.setUserId(userId);
        job.setStatus("NEW");
        job.setNewToday(false);
        job.setCapturedAt(System.currentTimeMillis());
        job.setSource(job.getSource() != null ? job.getSource() : "Bookmarklet");

        // Auto-score if default resume exists
        try {
            UserSettings settings = userRepository.getSettings(userId);
            if (settings.getDefaultResumeId() != null && settings.getEncryptedGeminiKey() != null) {
                ParsedResume resume = resumeRepository.findById(userId, settings.getDefaultResumeId());
                if (resume != null) {
                    String apiKey = encryptionService.decrypt(settings.getEncryptedGeminiKey());
                    JobMatchResult match = geminiService.analyzeJobMatch(resume, job, apiKey);
                    job.setMatchScore(match.getMatchScore());
                    job.setMatchedSkills(match.getMatchedSkills());
                    job.setMissingSkills(match.getMissingSkills());
                }
            }
        } catch (Exception ignored) {}

        JobPost saved = jobRepository.save(userId, job);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<JobPost>> list(Authentication authentication) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(jobRepository.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPost> getById(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        JobPost job = jobRepository.findById(userId, id);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id,
                                              @RequestBody Map<String, String> req,
                                              Authentication authentication) {
        String userId = getUserId(authentication);
        String status = req.get("status");
        if (status == null) return ResponseEntity.badRequest().build();
        jobRepository.updateStatus(userId, id, status);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<Void> updateNotes(@PathVariable String id,
                                             @RequestBody Map<String, String> req,
                                             Authentication authentication) {
        String userId = getUserId(authentication);
        String notes = req.get("notes");
        jobRepository.updateNotes(userId, id, notes);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        jobRepository.deleteJob(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(Authentication authentication) {
        String userId = getUserId(authentication);
        UserSettings settings = userRepository.getSettings(userId);
        String keywords = settings.getJobKeywords();
        String location = settings.getLocation();
        if (keywords == null || keywords.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No job keywords configured", "count", 0));
        }
        List<JobPost> jobs = aggregatorService.fetchJobs(keywords, location, userId);
        int saved = 0;
        for (JobPost job : jobs) {
            try {
                jobRepository.save(userId, job);
                saved++;
            } catch (Exception ignored) {}
        }
        if (saved > 0) {
            pushService.notifyAll(userId, "{\"title\":\"JARUS\",\"body\":\"" + saved + " new jobs found today!\"}");
        }
        return ResponseEntity.ok(Map.of("message", "Scan complete", "count", saved));
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
