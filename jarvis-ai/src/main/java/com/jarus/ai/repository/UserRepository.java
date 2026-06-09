package com.jarus.ai.repository;

import com.jarus.ai.model.UserProfile;
import com.jarus.ai.model.UserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    @Autowired
    private UserJpaRepository userJpa;

    @Autowired
    private SettingsJpaRepository settingsJpa;

    public void saveProfile(String userId, UserProfile p) {
        p.setUserId(userId);
        p.setLastLoginAt(System.currentTimeMillis());
        if (p.getCreatedAt() == 0) p.setCreatedAt(System.currentTimeMillis());
        userJpa.save(p);
    }

    public UserProfile getProfile(String userId) {
        return userJpa.findById(userId).orElse(null);
    }

    public void saveSettings(String userId, UserSettings s) {
        s.setUserId(userId);
        settingsJpa.save(s);
    }

    public UserSettings getSettings(String userId) {
        return settingsJpa.findById(userId).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUserId(userId);
            s.setScanTimeHour(8);
            return s;
        });
    }

    public void deleteUser(String userId) {
        userJpa.deleteById(userId);
        settingsJpa.deleteById(userId);
    }

    public List<UserProfile> getAllUsers() {
        return userJpa.findAll();
    }
}
