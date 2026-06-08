package com.jarus.ai.model;

public class ResumeSection {
    private String name;
    private String originalContent;
    private String modifiedContent;
    private boolean wasModified;
    private String changeReason;

    public ResumeSection() {}

    public ResumeSection(String name, String originalContent) {
        this.name = name;
        this.originalContent = originalContent;
        this.modifiedContent = originalContent;
        this.wasModified = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
    public String getModifiedContent() { return modifiedContent; }
    public void setModifiedContent(String modifiedContent) { this.modifiedContent = modifiedContent; }
    public boolean isWasModified() { return wasModified; }
    public void setWasModified(boolean wasModified) { this.wasModified = wasModified; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
