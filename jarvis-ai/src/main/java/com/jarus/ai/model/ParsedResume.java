package com.jarus.ai.model;

import com.jarus.ai.config.JpaConverters;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parsed_resumes")
public class ParsedResume {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String fileName;
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String fullText;

    @Convert(converter = JpaConverters.ResumeSectionListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ResumeSection> sections = new ArrayList<>();

    private String gcsPath; // Cloudinary public ID
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
