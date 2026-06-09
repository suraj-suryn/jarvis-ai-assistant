package com.jarus.ai.repository;

import com.jarus.ai.model.TailoredResume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class TailoredResumeRepository {

    @Autowired
    private TailoredResumeJpaRepository jpa;

    public TailoredResume save(String userId, TailoredResume t) {
        if (t.getId() == null) t.setId(UUID.randomUUID().toString());
        t.setUserId(userId);
        return jpa.save(t);
    }

    public TailoredResume findById(String userId, String id) {
        return jpa.findByIdAndUserId(id, userId).orElse(null);
    }

    public List<TailoredResume> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(String userId, String id) {
        jpa.findByIdAndUserId(id, userId).ifPresent(jpa::delete);
    }

    @Transactional
    public void deleteAll(String userId) {
        jpa.deleteByUserId(userId);
    }
}
