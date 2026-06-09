package com.jarus.ai.repository;

import com.jarus.ai.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobJpaRepository extends JpaRepository<JobPost, String> {
    List<JobPost> findByUserIdOrderByCapturedAtDesc(String userId);
    Optional<JobPost> findByIdAndUserId(String id, String userId);
    void deleteByIdAndUserId(String id, String userId);
}
