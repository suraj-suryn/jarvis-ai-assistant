package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.jarus.ai.model.UserProfile;
import com.jarus.ai.model.UserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";

    public void saveProfile(String userId, UserProfile p) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", p.getUserId());
            map.put("email", p.getEmail());
            map.put("displayName", p.getDisplayName());
            map.put("pictureUrl", p.getPictureUrl());
            map.put("createdAt", p.getCreatedAt() > 0 ? p.getCreatedAt() : System.currentTimeMillis());
            map.put("lastLoginAt", System.currentTimeMillis());
            firestore.collection(USERS).document(userId).set(map).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user profile", e);
        }
    }

    public UserProfile getProfile(String userId) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId).get().get();
            if (!doc.exists()) return null;
            Map<String, Object> m = doc.getData();
            UserProfile p = new UserProfile();
            p.setUserId((String) m.get("userId"));
            p.setEmail((String) m.get("email"));
            p.setDisplayName((String) m.get("displayName"));
            p.setPictureUrl((String) m.get("pictureUrl"));
            p.setCreatedAt(toLong(m.get("createdAt")));
            p.setLastLoginAt(toLong(m.get("lastLoginAt")));
            return p;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get user profile", e);
        }
    }

    public void saveSettings(String userId, UserSettings s) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            if (s.getEncryptedGeminiKey() != null) map.put("encryptedGeminiKey", s.getEncryptedGeminiKey());
            if (s.getDefaultResumeId() != null) map.put("defaultResumeId", s.getDefaultResumeId());
            if (s.getJobKeywords() != null) map.put("jobKeywords", s.getJobKeywords());
            if (s.getLocation() != null) map.put("location", s.getLocation());
            if (s.getEnabledSources() != null) map.put("enabledSources", s.getEnabledSources());
            map.put("scanTimeHour", s.getScanTimeHour() > 0 ? s.getScanTimeHour() : 8);
            if (s.getGmailAccessToken() != null) map.put("gmailAccessToken", s.getGmailAccessToken());
            if (s.getGmailRefreshToken() != null) map.put("gmailRefreshToken", s.getGmailRefreshToken());
            firestore.collection(USERS).document(userId).collection("settings").document("config").set(map).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save settings", e);
        }
    }

    @SuppressWarnings("unchecked")
    public UserSettings getSettings(String userId) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId)
                    .collection("settings").document("config").get().get();
            UserSettings s = new UserSettings();
            s.setUserId(userId);
            if (!doc.exists()) {
                s.setScanTimeHour(8);
                return s;
            }
            Map<String, Object> m = doc.getData();
            s.setEncryptedGeminiKey((String) m.get("encryptedGeminiKey"));
            s.setDefaultResumeId((String) m.get("defaultResumeId"));
            s.setJobKeywords((String) m.get("jobKeywords"));
            s.setLocation((String) m.get("location"));
            Object src = m.get("enabledSources");
            if (src instanceof List) s.setEnabledSources((List<String>) src);
            s.setScanTimeHour(m.get("scanTimeHour") != null ? ((Number) m.get("scanTimeHour")).intValue() : 8);
            s.setGmailAccessToken((String) m.get("gmailAccessToken"));
            s.setGmailRefreshToken((String) m.get("gmailRefreshToken"));
            return s;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get settings", e);
        }
    }

    public void deleteUser(String userId) {
        try {
            firestore.collection(USERS).document(userId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public List<UserProfile> getAllUsers() {
        try {
            List<UserProfile> users = new ArrayList<>();
            for (DocumentSnapshot doc : firestore.collection(USERS).get().get().getDocuments()) {
                Map<String, Object> m = doc.getData();
                UserProfile p = new UserProfile();
                p.setUserId(doc.getId());
                p.setEmail((String) m.get("email"));
                p.setDisplayName((String) m.get("displayName"));
                p.setPictureUrl((String) m.get("pictureUrl"));
                p.setLastLoginAt(toLong(m.get("lastLoginAt")));
                users.add(p);
            }
            return users;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list users", e);
        }
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
