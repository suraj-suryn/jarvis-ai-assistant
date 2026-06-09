package com.jarus.ai.repository;

import com.jarus.ai.model.ParsedResume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeJpaRepository extends JpaRepository<ParsedResume, String> {
    List<ParsedResume> findByUserId(String userId);
    Optional<ParsedResume> findByIdAndUserId(String id, String userId);
}
