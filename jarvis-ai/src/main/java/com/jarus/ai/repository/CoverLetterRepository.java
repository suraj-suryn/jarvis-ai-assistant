package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jarus.ai.model.CoverLetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class CoverLetterRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";
    private static final String COVER_LETTERS = "coverLetters";

    public CoverLetter save(String userId, CoverLetter cl) {
        try {
            if (cl.getId() == null) cl.setId(UUID.randomUUID().toString());
            cl.setUserId(userId);
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("jobId", cl.getJobId());
            map.put("resumeId", cl.getResumeId());
            map.put("content", cl.getContent() != null ? cl.getContent() : "");
            map.put("gcsPdfPath", cl.getGcsPdfPath());
            map.put("gcsDocxPath", cl.getGcsDocxPath());
            map.put("createdAt", cl.getCreatedAt() > 0 ? cl.getCreatedAt() : System.currentTimeMillis());
            firestore.collection(USERS).document(userId).collection(COVER_LETTERS)
                    .document(cl.getId()).set(map).get();
            return cl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save cover letter", e);
        }
    }

    public CoverLetter findById(String userId, String id) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId)
                    .collection(COVER_LETTERS).document(id).get().get();
            if (!doc.exists()) return null;
            return fromMap(doc.getId(), doc.getData());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get cover letter", e);
        }
    }

    public List<CoverLetter> findByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(COVER_LETTERS).get().get().getDocuments();
            List<CoverLetter> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) list.add(fromMap(doc.getId(), doc.getData()));
            return list;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list cover letters", e);
        }
    }

    public void delete(String userId, String id) {
        try {
            firestore.collection(USERS).document(userId).collection(COVER_LETTERS)
                    .document(id).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete cover letter", e);
        }
    }

    public void deleteAll(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(COVER_LETTERS).get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) doc.getReference().delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all cover letters", e);
        }
    }

    private CoverLetter fromMap(String id, Map<String, Object> m) {
        CoverLetter cl = new CoverLetter();
        cl.setId(id);
        cl.setUserId((String) m.get("userId"));
        cl.setJobId((String) m.get("jobId"));
        cl.setResumeId((String) m.get("resumeId"));
        cl.setContent((String) m.get("content"));
        cl.setGcsPdfPath((String) m.get("gcsPdfPath"));
        cl.setGcsDocxPath((String) m.get("gcsDocxPath"));
        cl.setCreatedAt(m.get("createdAt") != null ? ((Number) m.get("createdAt")).longValue() : 0);
        return cl;
    }
}
