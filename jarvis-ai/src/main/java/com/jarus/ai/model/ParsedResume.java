package com.jarus.ai.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedResume {
    private String id;
    private String userId;
    private String fileName;
    private String fileType; // PDF, DOCX, TXT
    private String fullText;
    private List<ResumeSection> sections = new ArrayList<>();
    private String gcsPath;
    private long uploadedAt;

    public ParsedResume() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }
    public List<ResumeSection> getSections() { return sections; }
    public void setSections(List<ResumeSection> sections) { this.sections = sections; }
    public String getGcsPath() { return gcsPath; }
    public void setGcsPath(String gcsPath) { this.gcsPath = gcsPath; }
    public long getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }
}
