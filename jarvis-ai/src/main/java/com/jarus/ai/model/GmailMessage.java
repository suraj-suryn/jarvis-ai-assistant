package com.jarus.ai.model;

public class GmailMessage {
    private String id;
    private String threadId;
    private String from;
    private String subject;
    private String snippet;
    private String body;
    private String tag; // RECRUITER, APPLIED, INTERVIEW, REJECTION, OTHER
    private long receivedAt;

    public GmailMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(long receivedAt) { this.receivedAt = receivedAt; }
}
