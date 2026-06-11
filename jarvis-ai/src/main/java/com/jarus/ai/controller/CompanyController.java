package com.jarus.ai.controller;

import com.jarus.ai.model.CompanyResearch;
import com.jarus.ai.model.JobPost;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.JobRepository;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.security.EncryptionService;
import com.jarus.ai.service.CompanyResearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired private CompanyResearchService researchService;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EncryptionService encryptionService;

    @GetMapping("/research")
    public ResponseEntity<?> research(@RequestParam String company,
                                       @RequestParam(required = false) String jobTitle,
                                       @RequestParam(required = false) String jobId,
                                       Authentication authentication) {
        String userId = getUserId(authentication);
        UserSettings settings = userRepository.getSettings(userId);
        if (settings.getEncryptedGeminiKey() == null) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Gemini API key not configured");
        }
        String apiKey;
        try {
            apiKey = encryptionService.decrypt(settings.getEncryptedGeminiKey());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve API key");
        }

        String jd = null;
        String resolvedTitle = jobTitle;
        if (jobId != null) {
            JobPost job = jobRepository.findById(userId, jobId);
            if (job != null) {
                jd = job.getDescription();
                if (resolvedTitle == null) resolvedTitle = job.getTitle();
                if (company == null || company.isEmpty()) company = job.getCompany();
            }
        }

        CompanyResearch result;
        try {
            result = researchService.research(company, resolvedTitle, jd, apiKey);
        } catch (com.jarus.ai.exception.GeminiRateLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
