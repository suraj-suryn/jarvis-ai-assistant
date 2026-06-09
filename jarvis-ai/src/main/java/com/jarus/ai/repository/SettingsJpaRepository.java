package com.jarus.ai.repository;

import com.jarus.ai.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsJpaRepository extends JpaRepository<UserSettings, String> {
}
