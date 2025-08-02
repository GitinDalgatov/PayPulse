package com.paypulse.notification.kafka;

import com.paypulse.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoginEventConsumer {
    private final NotificationService service;
    public LoginEventConsumer(NotificationService service) {
        this.service = service;
    }

    @KafkaListener(topics = "login.events")
    public void onLogin(String userId) {
        service.createNotification(UUID.fromString(userId), "LOGIN", "User logged in");
    }
}