package com.jarus.ai.controller;

import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.security.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired private UserRepository userRepository;
    @Autowired private EncryptionService encryptionService;

    @PostMapping("/gemini-key")
    public ResponseEntity<Void> saveGeminiKey(@RequestBody Map<String, String> req,
                                               Authentication authentication) {
        String userId = getUserId(authentication);
        String rawKey = req.get("apiKey");
        if (rawKey == null || rawKey.isBlank()) return ResponseEntity.badRequest().build();
        UserSettings settings = userRepository.getSettings(userId);
        settings.setEncryptedGeminiKey(encryptionService.encrypt(rawKey));
        userRepository.saveSettings(userId, settings);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/gemini-key/status")
    public ResponseEntity<Map<String, Boolean>> geminiKeyStatus(Authentication authentication) {
        String userId = getUserId(authentication);
        UserSettings settings = userRepository.getSettings(userId);
        boolean configured = settings.getEncryptedGeminiKey() != null;
        return ResponseEntity.ok(Map.of("configured", configured));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(Authentication authentication) {
        String userId = getUserId(authentication);
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        UserSettings settings = userRepository.getSettings(userId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("userId", userId);
        response.put("email", attrs.get("email"));
        response.put("name", attrs.get("name"));
        response.put("picture", attrs.get("picture"));
        response.put("jobKeywords", settings.getJobKeywords());
        response.put("location", settings.getLocation());
        response.put("enabledSources", settings.getEnabledSources());
        response.put("scanTimeHour", settings.getScanTimeHour());
        response.put("defaultResumeId", settings.getDefaultResumeId());
        response.put("geminiKeyConfigured", settings.getEncryptedGeminiKey() != null);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Void> saveSettings(@RequestBody Map<String, Object> req,
                                              Authentication authentication) {
        String userId = getUserId(authentication);
        UserSettings settings = userRepository.getSettings(userId);
        if (req.get("jobKeywords") != null) settings.setJobKeywords(req.get("jobKeywords").toString());
        if (req.get("location") != null) settings.setLocation(req.get("location").toString());
        if (req.get("scanTimeHour") != null) settings.setScanTimeHour(Integer.parseInt(req.get("scanTimeHour").toString()));
        if (req.get("defaultResumeId") != null) settings.setDefaultResumeId(req.get("defaultResumeId").toString());
        if (req.get("enabledSources") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) req.get("enabledSources");
            settings.setEnabledSources(sources);
        }
        userRepository.saveSettings(userId, settings);
        return ResponseEntity.ok().build();
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
