package com.jarus.ai.model;

import com.jarus.ai.config.JpaConverters;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(length = 2000)
    private String encryptedGeminiKey;
    private String defaultResumeId;

    @Column(length = 2000)
    private String jobKeywords;
    private String location;

    @Convert(converter = JpaConverters.StringListConverter.class)
    @Column(name = "enabled_sources", length = 1000)
    private List<String> enabledSources = new ArrayList<>();

    private int scanTimeHour = 8;

    @Column(length = 2000)
    private String gmailAccessToken;  // encrypted

    @Column(length = 2000)
    private String gmailRefreshToken; // encrypted

    public UserSettings() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEncryptedGeminiKey() { return encryptedGeminiKey; }
    public void setEncryptedGeminiKey(String encryptedGeminiKey) { this.encryptedGeminiKey = encryptedGeminiKey; }
    public String getDefaultResumeId() { return defaultResumeId; }
    public void setDefaultResumeId(String defaultResumeId) { this.defaultResumeId = defaultResumeId; }
    public String getJobKeywords() { return jobKeywords; }
    public void setJobKeywords(String jobKeywords) { this.jobKeywords = jobKeywords; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public List<String> getEnabledSources() { return enabledSources; }
    public void setEnabledSources(List<String> enabledSources) { this.enabledSources = enabledSources; }
    public int getScanTimeHour() { return scanTimeHour; }
    public void setScanTimeHour(int scanTimeHour) { this.scanTimeHour = scanTimeHour; }
    public String getGmailAccessToken() { return gmailAccessToken; }
    public void setGmailAccessToken(String gmailAccessToken) { this.gmailAccessToken = gmailAccessToken; }
    public String getGmailRefreshToken() { return gmailRefreshToken; }
    public void setGmailRefreshToken(String gmailRefreshToken) { this.gmailRefreshToken = gmailRefreshToken; }
}
