package com.jarus.ai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class AdminConfigRepository {

    @Autowired
    private Firestore firestore;

    private static final String CONFIG = "config";
    private static final String ALLOWED_EMAILS_DOC = "allowedEmails";

    @SuppressWarnings("unchecked")
    public List<String> getAllowedEmails() {
        try {
            DocumentSnapshot doc = firestore.collection(CONFIG).document(ALLOWED_EMAILS_DOC).get().get();
            if (!doc.exists()) return new ArrayList<>();
            Object emails = doc.getData().get("emails");
            if (emails instanceof List) return (List<String>) emails;
            return new ArrayList<>();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get allowed emails", e);
        }
    }

    public void addEmail(String email) {
        try {
            List<String> emails = getAllowedEmails();
            if (!emails.contains(email)) {
                emails.add(email);
                saveEmails(emails);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add email", e);
        }
    }

    public void removeEmail(String email) {
        try {
            List<String> emails = getAllowedEmails();
            emails.remove(email);
            saveEmails(emails);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove email", e);
        }
    }

    private void saveEmails(List<String> emails) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("emails", emails);
            firestore.collection(CONFIG).document(ALLOWED_EMAILS_DOC).set(map).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save emails", e);
        }
    }
}
