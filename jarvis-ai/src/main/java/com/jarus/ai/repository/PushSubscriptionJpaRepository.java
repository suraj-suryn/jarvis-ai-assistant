package com.jarus.ai.repository;

import com.jarus.ai.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscription, String> {
    List<PushSubscription> findByUserId(String userId);
    void deleteByUserId(String userId);
}
