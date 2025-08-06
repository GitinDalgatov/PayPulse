package com.paypulse.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.common.TransactionCreatedEvent;
import com.paypulse.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCreatedConsumer {

    private final NotificationService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction.created")
    public void onTransaction(String payload) {
        try {
            TransactionCreatedEvent event = objectMapper.readValue(payload, TransactionCreatedEvent.class);
            service.createNotification(event.toUserId(), event.type(), "Вам поступил перевод");
            service.createNotification(event.fromUserId(), event.type(), "Вы отправили перевод");
            log.info("Transaction notifications created: from={}, to={}, amount={}",
                    event.fromUserId(), event.toUserId(), event.amount());
        } catch (Exception e) {
            log.error("Failed to process transaction created event: payload={}", payload, e);
        }
    }
}