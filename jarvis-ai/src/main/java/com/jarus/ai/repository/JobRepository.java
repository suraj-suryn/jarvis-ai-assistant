package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jarus.ai.model.JobPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class JobRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";
    private static final String JOBS = "jobs";

    public JobPost save(String userId, JobPost job) {
        try {
            if (job.getId() == null) job.setId(UUID.randomUUID().toString());
            firestore.collection(USERS).document(userId).collection(JOBS)
                    .document(job.getId()).set(toMap(job)).get();
            return job;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save job", e);
        }
    }

    public List<JobPost> findByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(JOBS).orderBy("capturedAt", Query.Direction.DESCENDING)
                    .get().get().getDocuments();
            List<JobPost> jobs = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) jobs.add(fromMap(doc.getId(), doc.getData()));
            return jobs;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list jobs", e);
        }
    }

    public JobPost findById(String userId, String jobId) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId)
                    .collection(JOBS).document(jobId).get().get();
            if (!doc.exists()) return null;
            return fromMap(doc.getId(), doc.getData());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get job", e);
        }
    }

    public void updateStatus(String userId, String jobId, String status) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("status", status);
            if ("APPLIED".equals(status)) update.put("appliedAt", System.currentTimeMillis());
            firestore.collection(USERS).document(userId).collection(JOBS)
                    .document(jobId).update(update).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update job status", e);
        }
    }

    public void updateNotes(String userId, String jobId, String notes) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("interviewNotes", notes);
            firestore.collection(USERS).document(userId).collection(JOBS)
                    .document(jobId).update(update).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update interview notes", e);
        }
    }

    public void updateMatchScore(String userId, String jobId, int score,
                                  List<String> matched, List<String> missing) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("matchScore", score);
            update.put("matchedSkills", matched);
            update.put("missingSkills", missing);
            firestore.collection(USERS).document(userId).collection(JOBS)
                    .document(jobId).update(update).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update match score", e);
        }
    }

    public void deleteJob(String userId, String jobId) {
        try {
            firestore.collection(USERS).document(userId).collection(JOBS)
                    .document(jobId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete job", e);
        }
    }

    public void deleteAllJobs(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(JOBS).get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) doc.getReference().delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all jobs", e);
        }
    }

    @SuppressWarnings("unchecked")
    private JobPost fromMap(String id, Map<String, Object> m) {
        JobPost j = new JobPost();
        j.setId(id);
        j.setUserId((String) m.get("userId"));
        j.setTitle((String) m.get("title"));
        j.setCompany((String) m.get("company"));
        j.setDescription((String) m.get("description"));
        j.setUrl((String) m.get("url"));
        j.setSource((String) m.get("source"));
        j.setStatus(m.get("status") != null ? (String) m.get("status") : "NEW");
        j.setMatchScore(m.get("matchScore") != null ? ((Number) m.get("matchScore")).intValue() : 0);
        j.setInterviewNotes((String) m.get("interviewNotes"));
        j.setNewToday(Boolean.TRUE.equals(m.get("newToday")));
        j.setCapturedAt(toLong(m.get("capturedAt")));
        j.setAppliedAt(toLong(m.get("appliedAt")));
        j.setInterviewDate(toLong(m.get("interviewDate")));
        Object matched = m.get("matchedSkills");
        if (matched instanceof List) j.setMatchedSkills((List<String>) matched);
        Object missing = m.get("missingSkills");
        if (missing instanceof List) j.setMissingSkills((List<String>) missing);
        return j;
    }

    private Map<String, Object> toMap(JobPost j) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", j.getUserId());
        map.put("title", j.getTitle() != null ? j.getTitle() : "");
        map.put("company", j.getCompany() != null ? j.getCompany() : "");
        map.put("description", j.getDescription() != null ? j.getDescription() : "");
        map.put("url", j.getUrl() != null ? j.getUrl() : "");
        map.put("source", j.getSource() != null ? j.getSource() : "");
        map.put("status", j.getStatus() != null ? j.getStatus() : "NEW");
        map.put("matchScore", j.getMatchScore());
        map.put("matchedSkills", j.getMatchedSkills());
        map.put("missingSkills", j.getMissingSkills());
        map.put("interviewNotes", j.getInterviewNotes());
        map.put("newToday", j.isNewToday());
        map.put("capturedAt", j.getCapturedAt() > 0 ? j.getCapturedAt() : System.currentTimeMillis());
        map.put("appliedAt", j.getAppliedAt());
        map.put("interviewDate", j.getInterviewDate());
        return map;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
