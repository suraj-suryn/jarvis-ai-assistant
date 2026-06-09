package com.jarus.ai.repository;

import com.jarus.ai.model.PushSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class PushSubscriptionRepository {

    @Autowired
    private PushSubscriptionJpaRepository jpa;

    public PushSubscription save(String userId, PushSubscription sub) {
        if (sub.getId() == null) sub.setId(UUID.randomUUID().toString());
        sub.setUserId(userId);
        return jpa.save(sub);
    }

    public List<PushSubscription> findByUserId(String userId) {
        return jpa.findByUserId(userId);
    }

    @Transactional
    public void delete(String userId, String subId) {
        jpa.findById(subId).filter(s -> userId.equals(s.getUserId())).ifPresent(jpa::delete);
    }

    @Transactional
    public void deleteAll(String userId) {
        jpa.deleteByUserId(userId);
    }
}
