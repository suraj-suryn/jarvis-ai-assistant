package com.jarus.ai.model;

import java.util.ArrayList;
import java.util.List;

public class TailoredResume {
    private String id;
    private String userId;
    private String originalResumeId;
    private String jobId;
    private List<ResumeSection> modifiedSections = new ArrayList<>();
    private List<String> changesSummary = new ArrayList<>();
    private String gcsPdfPath;
    private String gcsDocxPath;
    private long createdAt;

    public TailoredResume() {
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOriginalResumeId() { return originalResumeId; }
    public void setOriginalResumeId(String originalResumeId) { this.originalResumeId = originalResumeId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public List<ResumeSection> getModifiedSections() { return modifiedSections; }
    public void setModifiedSections(List<ResumeSection> modifiedSections) { this.modifiedSections = modifiedSections; }
    public List<String> getChangesSummary() { return changesSummary; }
    public void setChangesSummary(List<String> changesSummary) { this.changesSummary = changesSummary; }
    public String getGcsPdfPath() { return gcsPdfPath; }
    public void setGcsPdfPath(String gcsPdfPath) { this.gcsPdfPath = gcsPdfPath; }
    public String getGcsDocxPath() { return gcsDocxPath; }
    public void setGcsDocxPath(String gcsDocxPath) { this.gcsDocxPath = gcsDocxPath; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
