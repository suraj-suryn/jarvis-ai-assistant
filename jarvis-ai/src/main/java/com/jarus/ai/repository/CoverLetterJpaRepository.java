package com.jarus.ai.repository;

import com.jarus.ai.model.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverLetterJpaRepository extends JpaRepository<CoverLetter, String> {
    List<CoverLetter> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<CoverLetter> findByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
