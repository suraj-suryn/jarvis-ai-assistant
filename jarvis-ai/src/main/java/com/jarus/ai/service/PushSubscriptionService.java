package com.jarus.ai.service;

import com.jarus.ai.model.PushSubscription;
import com.jarus.ai.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushSubscriptionService {

    @Autowired
    private PushSubscriptionRepository repository;

    @Autowired
    private WebPushService webPushService;

    public PushSubscription subscribe(String userId, PushSubscription sub) {
        sub.setUserId(userId);
        return repository.save(userId, sub);
    }

    public void unsubscribe(String userId, String subId) {
        repository.delete(userId, subId);
    }

    public void notifyAll(String userId, String message) {
        List<PushSubscription> subs = repository.findByUserId(userId);
        for (PushSubscription sub : subs) {
            webPushService.sendNotification(sub, message);
        }
    }

    public List<PushSubscription> getSubscriptions(String userId) {
        return repository.findByUserId(userId);
    }
}
