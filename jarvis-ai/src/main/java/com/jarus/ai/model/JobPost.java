package com.jarus.ai.model;

import com.jarus.ai.config.JpaConverters;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_posts")
public class JobPost {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String title;
    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2000)
    private String url;
    private String source;
    private String status;
    private int matchScore;

    @Convert(converter = JpaConverters.StringListConverter.class)
    @Column(name = "matched_skills", length = 2000)
    private List<String> matchedSkills = new ArrayList<>();

    @Convert(converter = JpaConverters.StringListConverter.class)
    @Column(name = "missing_skills", length = 2000)
    private List<String> missingSkills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String interviewNotes;

    private boolean newToday;
    private long capturedAt;
    private long appliedAt;
    private long interviewDate;

    // REMOTE / HYBRID / ONSITE — populated by aggregator sources
    @Column(length = 20)
    private String workType;

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
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
}
