package com.jarus.ai.repository;

import com.jarus.ai.model.CoverLetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class CoverLetterRepository {

    @Autowired
    private CoverLetterJpaRepository jpa;

    public CoverLetter save(String userId, CoverLetter cl) {
        if (cl.getId() == null) cl.setId(UUID.randomUUID().toString());
        cl.setUserId(userId);
        return jpa.save(cl);
    }

    public CoverLetter findById(String userId, String id) {
        return jpa.findByIdAndUserId(id, userId).orElse(null);
    }

    public List<CoverLetter> findByUserId(String userId) {
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
