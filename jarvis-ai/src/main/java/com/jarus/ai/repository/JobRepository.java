package com.jarus.ai.repository;

import com.jarus.ai.model.JobPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class JobRepository {

    @Autowired
    private JobJpaRepository jpa;

    private static final String USERS = "users";
    private static final String JOBS = "jobs";

    public JobPost save(String userId, JobPost job) {
        if (job.getId() == null) job.setId(UUID.randomUUID().toString());
        job.setUserId(userId);
        return jpa.save(job);
    }

    public List<JobPost> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCapturedAtDesc(userId);
    }

    public JobPost findById(String userId, String jobId) {
        return jpa.findByIdAndUserId(jobId, userId).orElse(null);
    }

    public void updateStatus(String userId, String jobId, String status) {
        JobPost job = findById(userId, jobId);
        if (job == null) return;
        job.setStatus(status);
        if ("APPLIED".equals(status)) job.setAppliedAt(System.currentTimeMillis());
        jpa.save(job);
    }

    public void updateNotes(String userId, String jobId, String notes) {
        JobPost job = findById(userId, jobId);
        if (job == null) return;
        job.setInterviewNotes(notes);
        jpa.save(job);
    }

    public void updateMatchScore(String userId, String jobId, int score,
                                  List<String> matched, List<String> missing) {
        JobPost job = findById(userId, jobId);
        if (job == null) return;
        job.setMatchScore(score);
        job.setMatchedSkills(matched);
        job.setMissingSkills(missing);
        jpa.save(job);
    }

    @Transactional
    public void deleteJob(String userId, String jobId) {
        jpa.deleteByIdAndUserId(jobId, userId);
    }

    @Transactional
    public void deleteAllJobs(String userId) {
        jpa.deleteAll(jpa.findByUserIdOrderByCapturedAtDesc(userId));
    }
}
