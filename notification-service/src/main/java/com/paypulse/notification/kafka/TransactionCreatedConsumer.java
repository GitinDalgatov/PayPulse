package com.paypulse.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.common.TransactionCreatedEvent;
import com.paypulse.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class TransactionCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionCreatedConsumer.class);
    private final NotificationService service;
    private final ObjectMapper objectMapper;

    public TransactionCreatedConsumer(NotificationService service,
                                    @Autowired ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction.created")
    public void onTransaction(String payload) {
        try {
            TransactionCreatedEvent event = objectMapper.readValue(payload, TransactionCreatedEvent.class);
            service.createNotification(event.getToUserId(), event.getType(), "Вам поступил перевод");
            service.createNotification(event.getFromUserId(), event.getType(), "Вы отправили перевод");
            log.info("Transaction notifications created: from={}, to={}, amount={}", 
                event.getFromUserId(), event.getToUserId(), event.getAmount());
        } catch (Exception e) {
            log.error("Failed to process transaction created event: payload={}", payload, e);
        }
    }
}