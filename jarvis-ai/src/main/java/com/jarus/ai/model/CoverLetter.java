package com.jarus.ai.model;

public class CoverLetter {
    private String id;
    private String userId;
    private String jobId;
    private String resumeId;
    private String content;
    private String gcsPdfPath;
    private String gcsDocxPath;
    private long createdAt;

    public CoverLetter() {
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getGcsPdfPath() { return gcsPdfPath; }
    public void setGcsPdfPath(String gcsPdfPath) { this.gcsPdfPath = gcsPdfPath; }
    public String getGcsDocxPath() { return gcsDocxPath; }
    public void setGcsDocxPath(String gcsDocxPath) { this.gcsDocxPath = gcsDocxPath; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
