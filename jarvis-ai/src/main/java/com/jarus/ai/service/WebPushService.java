package com.jarus.ai.service;

import com.jarus.ai.model.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Security;

@Service
public class WebPushService {

    @Value("${vapid.public.key:}")
    private String vapidPublicKey;

    @Value("${vapid.private.key:}")
    private String vapidPrivateKey;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (!vapidPublicKey.isEmpty() && !vapidPrivateKey.isEmpty()) {
            try {
                pushService = new PushService(vapidPublicKey, vapidPrivateKey, "JARUS Job Assistant");
            } catch (Exception e) {
                // VAPID keys not configured — push will be no-op
            }
        }
    }

    public void sendNotification(PushSubscription sub, String payload) {
        if (pushService == null) return;
        try {
            nl.martijndwars.webpush.Subscription webPushSub =
                    new nl.martijndwars.webpush.Subscription(sub.getEndpoint(),
                            new nl.martijndwars.webpush.Subscription.Keys(sub.getP256dh(), sub.getAuth()));
            Notification notification = new Notification(webPushSub, payload);
            pushService.send(notification);
        } catch (Exception e) {
            // Non-fatal: individual push failure should not break the flow
        }
    }
}
