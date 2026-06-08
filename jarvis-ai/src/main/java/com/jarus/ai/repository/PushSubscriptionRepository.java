package com.jarus.ai.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.jarus.ai.model.PushSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class PushSubscriptionRepository {

    @Autowired
    private Firestore firestore;

    private static final String USERS = "users";
    private static final String PUSH_SUBS = "pushSubs";

    public PushSubscription save(String userId, PushSubscription sub) {
        try {
            if (sub.getId() == null) sub.setId(UUID.randomUUID().toString());
            sub.setUserId(userId);
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("endpoint", sub.getEndpoint());
            map.put("p256dh", sub.getP256dh());
            map.put("auth", sub.getAuth());
            map.put("createdAt", sub.getCreatedAt() > 0 ? sub.getCreatedAt() : System.currentTimeMillis());
            firestore.collection(USERS).document(userId).collection(PUSH_SUBS)
                    .document(sub.getId()).set(map).get();
            return sub;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save push subscription", e);
        }
    }

    public List<PushSubscription> findByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(PUSH_SUBS).get().get().getDocuments();
            List<PushSubscription> subs = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                Map<String, Object> m = doc.getData();
                PushSubscription sub = new PushSubscription();
                sub.setId(doc.getId());
                sub.setUserId(userId);
                sub.setEndpoint((String) m.get("endpoint"));
                sub.setP256dh((String) m.get("p256dh"));
                sub.setAuth((String) m.get("auth"));
                sub.setCreatedAt(m.get("createdAt") != null ? ((Number) m.get("createdAt")).longValue() : 0);
                subs.add(sub);
            }
            return subs;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list push subscriptions", e);
        }
    }

    public void delete(String userId, String subId) {
        try {
            firestore.collection(USERS).document(userId).collection(PUSH_SUBS)
                    .document(subId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete push subscription", e);
        }
    }

    public void deleteAll(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(USERS).document(userId)
                    .collection(PUSH_SUBS).get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) doc.getReference().delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all push subscriptions", e);
        }
    }
}
