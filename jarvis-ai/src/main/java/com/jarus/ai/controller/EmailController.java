package com.jarus.ai.controller;

import com.jarus.ai.model.GmailMessage;
import com.jarus.ai.service.GmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private GmailService gmailService;

    @GetMapping("/jobs")
    public ResponseEntity<?> fetchJobEmails(Authentication authentication) {
        try {
            List<GmailMessage> messages = gmailService.fetchJobEmails(authentication);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch emails: " + e.getMessage());
        }
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<?> getThread(@PathVariable String threadId, Authentication authentication) {
        try {
            List<GmailMessage> messages = gmailService.getThread(authentication, threadId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch thread: " + e.getMessage());
        }
    }

    @PostMapping("/tag")
    public ResponseEntity<Void> tagEmail(@RequestBody Map<String, String> req) {
        // Tag is computed client-side from auto-detection; this endpoint is a no-op placeholder
        // for future manual tag overrides stored in Firestore.
        return ResponseEntity.ok().build();
    }
}
