package com.paypulse.notification.service;

import com.paypulse.common.NotificationResponse;
import com.paypulse.notification.entity.Notification;
import com.paypulse.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public void createNotification(UUID userId, String type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .timestamp(Instant.now())
                .build();
        repository.save(notification);
    }

    public List<NotificationResponse> getNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId = UUID.fromString(auth.getName());
        return repository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getType(),
                        n.getMessage(),
                        n.getTimestamp()))
                .toList(); // Java 16+
    }
}