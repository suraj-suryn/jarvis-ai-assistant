package com.jarus.ai.controller;

import com.jarus.ai.model.UserProfile;
import com.jarus.ai.repository.AdminConfigRepository;
import com.jarus.ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private AdminConfigRepository adminConfigRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers() {
        List<String> allowedEmails = adminConfigRepository.getAllowedEmails();
        List<UserProfile> allProfiles = userRepository.getAllUsers();
        return ResponseEntity.ok(Map.of(
                "allowedEmails", allowedEmails,
                "users", allProfiles
        ));
    }

    @PostMapping("/users/add")
    public ResponseEntity<Void> addUser(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        adminConfigRepository.addEmail(email.trim().toLowerCase());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{email}")
    public ResponseEntity<Void> removeUser(@PathVariable String email) {
        adminConfigRepository.removeEmail(email);
        return ResponseEntity.noContent().build();
    }
}
