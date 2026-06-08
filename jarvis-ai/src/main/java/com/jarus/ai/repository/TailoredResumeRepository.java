package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jarus.ai.model.ResumeSection;
import com.jarus.ai.model.TailoredResume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class TailoredResumeRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";
    private static final String TAILORED = "tailored";

    public TailoredResume save(String userId, TailoredResume t) {
        try {
            if (t.getId() == null) t.setId(UUID.randomUUID().toString());
            t.setUserId(userId);
            firestore.collection(USERS).document(userId).collection(TAILORED)
                    .document(t.getId()).set(toMap(t)).get();
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save tailored resume", e);
        }
    }

    public TailoredResume findById(String userId, String id) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId)
                    .collection(TAILORED).document(id).get().get();
            if (!doc.exists()) return null;
            return fromMap(doc.getId(), doc.getData());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get tailored resume", e);
        }
    }

    public List<TailoredResume> findByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(TAILORED).get().get().getDocuments();
            List<TailoredResume> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) list.add(fromMap(doc.getId(), doc.getData()));
            return list;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list tailored resumes", e);
        }
    }

    public void delete(String userId, String id) {
        try {
            firestore.collection(USERS).document(userId).collection(TAILORED)
                    .document(id).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete tailored resume", e);
        }
    }

    public void deleteAll(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(TAILORED).get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) doc.getReference().delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all tailored resumes", e);
        }
    }

    @SuppressWarnings("unchecked")
    private TailoredResume fromMap(String id, Map<String, Object> m) {
        TailoredResume t = new TailoredResume();
        t.setId(id);
        t.setUserId((String) m.get("userId"));
        t.setOriginalResumeId((String) m.get("originalResumeId"));
        t.setJobId((String) m.get("jobId"));
        t.setGcsPdfPath((String) m.get("gcsPdfPath"));
        t.setGcsDocxPath((String) m.get("gcsDocxPath"));
        t.setCreatedAt(m.get("createdAt") != null ? ((Number) m.get("createdAt")).longValue() : 0);
        Object changes = m.get("changesSummary");
        if (changes instanceof List) t.setChangesSummary((List<String>) changes);
        Object sectionsObj = m.get("modifiedSections");
        if (sectionsObj instanceof List) {
            List<ResumeSection> sections = new ArrayList<>();
            for (Object sObj : (List<?>) sectionsObj) {
                if (sObj instanceof Map) {
                    Map<String, Object> sm = (Map<String, Object>) sObj;
                    ResumeSection sec = new ResumeSection();
                    sec.setName((String) sm.get("name"));
                    sec.setOriginalContent((String) sm.get("originalContent"));
                    sec.setModifiedContent((String) sm.get("modifiedContent"));
                    sec.setWasModified(Boolean.TRUE.equals(sm.get("wasModified")));
                    sec.setChangeReason((String) sm.get("changeReason"));
                    sections.add(sec);
                }
            }
            t.setModifiedSections(sections);
        }
        return t;
    }

    private Map<String, Object> toMap(TailoredResume t) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", t.getUserId());
        map.put("originalResumeId", t.getOriginalResumeId());
        map.put("jobId", t.getJobId());
        map.put("gcsPdfPath", t.getGcsPdfPath());
        map.put("gcsDocxPath", t.getGcsDocxPath());
        map.put("createdAt", t.getCreatedAt() > 0 ? t.getCreatedAt() : System.currentTimeMillis());
        map.put("changesSummary", t.getChangesSummary());
        List<Map<String, Object>> sections = new ArrayList<>();
        if (t.getModifiedSections() != null) {
            for (ResumeSection s : t.getModifiedSections()) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("name", s.getName());
                sm.put("originalContent", s.getOriginalContent());
                sm.put("modifiedContent", s.getModifiedContent());
                sm.put("wasModified", s.isWasModified());
                sm.put("changeReason", s.getChangeReason());
                sections.add(sm);
            }
        }
        map.put("modifiedSections", sections);
        return map;
    }
}
