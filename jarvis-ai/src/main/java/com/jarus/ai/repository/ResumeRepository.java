package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jarus.ai.model.ParsedResume;
import com.jarus.ai.model.ResumeSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class ResumeRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";
    private static final String RESUMES = "resumes";

    public ParsedResume save(String userId, ParsedResume resume) {
        try {
            if (resume.getId() == null) resume.setId(UUID.randomUUID().toString());
            resume.setUserId(userId);
            firestore.collection(USERS).document(userId).collection(RESUMES)
                    .document(resume.getId()).set(toMap(resume)).get();
            return resume;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save resume", e);
        }
    }

    public ParsedResume findById(String userId, String resumeId) {
        try {
            DocumentSnapshot doc = firestore.collection(USERS).document(userId)
                    .collection(RESUMES).document(resumeId).get().get();
            if (!doc.exists()) return null;
            return fromMap(doc.getId(), doc.getData());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get resume", e);
        }
    }

    public List<ParsedResume> findByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(RESUMES).get().get().getDocuments();
            List<ParsedResume> resumes = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) resumes.add(fromMap(doc.getId(), doc.getData()));
            return resumes;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list resumes", e);
        }
    }

    public void delete(String userId, String resumeId) {
        try {
            firestore.collection(USERS).document(userId).collection(RESUMES)
                    .document(resumeId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resume", e);
        }
    }

    public void deleteAll(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(RESUMES).get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) doc.getReference().delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resumes", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ParsedResume fromMap(String id, Map<String, Object> m) {
        ParsedResume r = new ParsedResume();
        r.setId(id);
        r.setUserId((String) m.get("userId"));
        r.setFileName((String) m.get("fileName"));
        r.setFileType((String) m.get("fileType"));
        r.setFullText((String) m.get("fullText"));
        r.setGcsPath((String) m.get("gcsPath"));
        r.setUploadedAt(m.get("uploadedAt") != null ? ((Number) m.get("uploadedAt")).longValue() : 0);
        // Sections stored as list of maps
        Object sectionsObj = m.get("sections");
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
            r.setSections(sections);
        }
        return r;
    }

    private Map<String, Object> toMap(ParsedResume r) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", r.getUserId());
        map.put("fileName", r.getFileName());
        map.put("fileType", r.getFileType());
        map.put("fullText", r.getFullText() != null ? r.getFullText() : "");
        map.put("gcsPath", r.getGcsPath());
        map.put("uploadedAt", r.getUploadedAt() > 0 ? r.getUploadedAt() : System.currentTimeMillis());
        List<Map<String, Object>> sections = new ArrayList<>();
        if (r.getSections() != null) {
            for (ResumeSection s : r.getSections()) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("name", s.getName());
                sm.put("originalContent", s.getOriginalContent());
                sm.put("modifiedContent", s.getModifiedContent());
                sm.put("wasModified", s.isWasModified());
                sm.put("changeReason", s.getChangeReason());
                sections.add(sm);
            }
        }
        map.put("sections", sections);
        return map;
    }
}
