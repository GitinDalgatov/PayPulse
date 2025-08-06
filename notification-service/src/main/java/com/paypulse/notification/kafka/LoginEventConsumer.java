package com.paypulse.notification.kafka;

import com.paypulse.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventConsumer {

    private final NotificationService service;

    @KafkaListener(topics = "login.events")
    public void onLogin(String userId) {
        try {
            service.createNotification(UUID.fromString(userId), "LOGIN", "User logged in");
            log.info("Login notification created for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to process login event: userId={}", userId, e);
        }
    }
}