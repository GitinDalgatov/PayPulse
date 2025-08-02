package com.paypulse.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.common.BalanceChangedEvent;
import com.paypulse.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class BalanceChangedConsumer {
    private static final Logger log = LoggerFactory.getLogger(BalanceChangedConsumer.class);
    private final NotificationService service;
    private final ObjectMapper objectMapper;

    public BalanceChangedConsumer(NotificationService service, 
                                @Autowired ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet.balance.changed")
    public void onBalanceChanged(String payload) {
        try {
            BalanceChangedEvent event = objectMapper.readValue(payload, BalanceChangedEvent.class);
            service.createNotification(event.getUserId(), event.getType(), event.getDescription());
            log.info("Balance changed notification created: userId={}, type={}", 
                event.getUserId(), event.getType());
        } catch (Exception e) {
            log.error("Failed to process balance changed event: payload={}", payload, e);
        }
    }
}