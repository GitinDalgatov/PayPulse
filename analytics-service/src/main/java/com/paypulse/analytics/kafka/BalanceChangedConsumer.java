package com.paypulse.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.analytics.entity.BalanceEvent;
import com.paypulse.analytics.repository.BalanceRepository;
import com.paypulse.common.BalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class BalanceChangedConsumer {
    private final BalanceRepository balanceRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "wallet.balance.changed", groupId = "paypulse-group")
    public void onBalanceChanged(String payload) {
        try {
            log.info("Analytics: received balance event: {}", payload);
            BalanceChangedEvent event = objectMapper.readValue(payload, BalanceChangedEvent.class);

            BalanceEvent analyticsEvent = new BalanceEvent(
                    UUID.randomUUID().toString(),
                    event.userId(),
                    event.type(),
                    event.description()
            );

            balanceRepository.save(analyticsEvent);
            log.info("Analytics: balance event consumed and saved: userId={}, type={}, id={}",
                    event.userId(), event.type(), analyticsEvent.id());
        } catch (Exception e) {
            log.error("Failed to process balance event: payload={}", payload, e);
        }
    }
} 