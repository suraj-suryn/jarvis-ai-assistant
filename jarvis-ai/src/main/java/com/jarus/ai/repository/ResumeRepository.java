package com.jarus.ai.repository;

import com.jarus.ai.model.ParsedResume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class ResumeRepository {

    @Autowired
    private ResumeJpaRepository jpa;

    public ParsedResume save(String userId, ParsedResume resume) {
        if (resume.getId() == null) resume.setId(UUID.randomUUID().toString());
        resume.setUserId(userId);
        if (resume.getUploadedAt() == 0) resume.setUploadedAt(System.currentTimeMillis());
        return jpa.save(resume);
    }

    public ParsedResume findById(String userId, String resumeId) {
        return jpa.findByIdAndUserId(resumeId, userId).orElse(null);
    }

    public List<ParsedResume> findByUserId(String userId) {
        return jpa.findByUserId(userId);
    }

    @Transactional
    public void delete(String userId, String resumeId) {
        jpa.findByIdAndUserId(resumeId, userId).ifPresent(jpa::delete);
    }

    @Transactional
    public void deleteAll(String userId) {
        jpa.deleteAll(jpa.findByUserId(userId));
    }
}
