package com.onthi.v_edu.push.service;

import com.onthi.v_edu.push.entity.PushSubscription;
import com.onthi.v_edu.push.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    @Value("${app.push.vapid.public-key}")
    private String publicKey;

    @Value("${app.push.vapid.private-key}")
    private String privateKey;

    @Value("${app.push.vapid.subject:mailto:admin@v-edu.com}")
    private String subject;

    private final PushSubscriptionRepository subscriptionRepository;
    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService(publicKey, privateKey, subject);
        } catch (Exception e) {
            log.error("Failed to initialize PushService: {}", e.getMessage());
        }
    }

    public void sendNotification(Integer userId, String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        
        String payload = String.format("{\"title\":\"%s\", \"body\":\"%s\"}", title, body);

        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload
                );
                pushService.send(notification);
                log.info("Push notification sent to user {} at endpoint {}", userId, sub.getEndpoint());
            } catch (Exception e) {
                log.error("Failed to send push notification to {}: {}", sub.getEndpoint(), e.getMessage());
                // If it fails with 410 Gone, we should delete the subscription
                if (e.getMessage().contains("410")) {
                    subscriptionRepository.delete(sub);
                }
            }
        }
    }
}
