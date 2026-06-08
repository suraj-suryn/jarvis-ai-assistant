package com.jarus.ai.controller;

import com.jarus.ai.model.PushSubscription;
import com.jarus.ai.service.PushSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    @Autowired
    private PushSubscriptionService pushSubscriptionService;

    @Value("${vapid.public.key:}")
    private String vapidPublicKey;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<PushSubscription> subscribe(@RequestBody PushSubscription sub,
                                                       Authentication authentication) {
        String userId = getUserId(authentication);
        PushSubscription saved = pushSubscriptionService.subscribe(userId, sub);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/unsubscribe/{id}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String id, Authentication authentication) {
        String userId = getUserId(authentication);
        pushSubscriptionService.unsubscribe(userId, id);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(Authentication auth) {
        return ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("sub").toString();
    }
}
