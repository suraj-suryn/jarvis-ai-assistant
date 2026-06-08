package com.jarus.ai.model;

import java.util.ArrayList;
import java.util.List;

public class JobPost {
    private String id;
    private String userId;
    private String title;
    private String company;
    private String description;
    private String url;
    private String source; // linkedin, naukri, indeed, remoteok, etc.
    private String status; // NEW, APPLIED, INTERVIEW, OFFER, REJECTED
    private int matchScore;
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private String interviewNotes;
    private boolean newToday;
    private long capturedAt;
    private long appliedAt;
    private long interviewDate;

    public JobPost() {
        this.status = "NEW";
        this.capturedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    public String getInterviewNotes() { return interviewNotes; }
    public void setInterviewNotes(String interviewNotes) { this.interviewNotes = interviewNotes; }
    public boolean isNewToday() { return newToday; }
    public void setNewToday(boolean newToday) { this.newToday = newToday; }
    public long getCapturedAt() { return capturedAt; }
    public void setCapturedAt(long capturedAt) { this.capturedAt = capturedAt; }
    public long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }
    public long getInterviewDate() { return interviewDate; }
    public void setInterviewDate(long interviewDate) { this.interviewDate = interviewDate; }
}
