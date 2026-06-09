package com.jarus.ai.repository;

import com.jarus.ai.model.TailoredResume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TailoredResumeJpaRepository extends JpaRepository<TailoredResume, String> {
    List<TailoredResume> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<TailoredResume> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
