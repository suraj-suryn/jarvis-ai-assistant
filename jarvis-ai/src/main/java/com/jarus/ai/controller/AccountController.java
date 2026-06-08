package com.jarus.ai.controller;

import com.jarus.ai.repository.*;
import com.jarus.ai.service.GcsStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired private UserRepository userRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private TailoredResumeRepository tailoredRepository;
    @Autowired private CoverLetterRepository coverLetterRepository;
    @Autowired private PushSubscriptionRepository pushRepository;
    @Autowired private GcsStorageService gcsService;

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        String userId = getUserId(authentication);

        // Delete GCS files
        try { gcsService.deleteFolder(userId + "/"); } catch (Exception ignored) {}

        // Delete Firestore data
        try { jobRepository.deleteAllJobs(userId); } catch (Exception ignored) {}
        try { resumeRepository.deleteAll(userId); } catch (Exception ignored) {}
        try { tailoredRepository.deleteAll(userId); } catch (Exception ignored) {}
        try { coverLetterRepository.deleteAll(userId); } catch (Exception ignored) {}
        try { pushRepository.deleteAll(userId); } catch (Exception ignored) {}
        try { userRepository.deleteUser(userId); } catch (Exception ignored) {}

        return ResponseEntity.noContent().build();
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
