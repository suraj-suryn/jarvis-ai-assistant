package com.jarus.ai.model;

import com.jarus.ai.config.JpaConverters;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_config")
public class AdminConfig {

    @Id
    private String id;

    @Convert(converter = JpaConverters.StringListConverter.class)
    @Column(name = "allowed_emails", length = 5000)
    private List<String> allowedEmails = new ArrayList<>();

    public AdminConfig() {}

    public AdminConfig(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getAllowedEmails() { return allowedEmails; }
    public void setAllowedEmails(List<String> allowedEmails) { this.allowedEmails = allowedEmails; }
}
