package com.paypulse.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.common.BalanceChangedEvent;
import com.paypulse.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceChangedConsumer {

    private final NotificationService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "wallet.balance.changed")
    public void onBalanceChanged(String payload) {
        try {
            BalanceChangedEvent event = objectMapper.readValue(payload, BalanceChangedEvent.class);
            service.createNotification(event.userId(), event.type(), event.description());
            log.info("Balance changed notification created: userId={}, type={}",
                    event.userId(), event.type());
        } catch (Exception e) {
            log.error("Failed to process balance changed event: payload={}", payload, e);
        }
    }
}