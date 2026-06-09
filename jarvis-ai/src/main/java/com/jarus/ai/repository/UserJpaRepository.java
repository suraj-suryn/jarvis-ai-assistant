package com.jarus.ai.repository;

import com.jarus.ai.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserProfile, String> {
}
