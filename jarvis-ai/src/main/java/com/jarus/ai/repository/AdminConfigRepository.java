package com.jarus.ai.repository;

import com.jarus.ai.model.AdminConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminConfigRepository {

    private static final String GLOBAL_ID = "global";

    @Autowired
    private AdminConfigJpaRepository jpa;

    public List<String> getAllowedEmails() {
        return jpa.findById(GLOBAL_ID)
                .map(AdminConfig::getAllowedEmails)
                .orElse(new ArrayList<>());
    }

    public void addEmail(String email) {
        AdminConfig config = jpa.findById(GLOBAL_ID).orElse(new AdminConfig(GLOBAL_ID));
        if (!config.getAllowedEmails().contains(email)) {
            config.getAllowedEmails().add(email);
            jpa.save(config);
        }
    }

    public void removeEmail(String email) {
        jpa.findById(GLOBAL_ID).ifPresent(config -> {
            config.getAllowedEmails().remove(email);
            jpa.save(config);
        });
    }
}
