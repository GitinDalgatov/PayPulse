package com.paypulse.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.analytics.entity.BalanceEvent;
import com.paypulse.analytics.repository.BalanceRepository;
import com.paypulse.common.BalanceChangedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Component
public class BalanceChangedConsumer {
    private static final Logger log = LoggerFactory.getLogger(BalanceChangedConsumer.class);
    private final BalanceRepository balanceRepository;
    private final ObjectMapper objectMapper;

    public BalanceChangedConsumer(BalanceRepository balanceRepository, 
                                @Autowired ObjectMapper objectMapper) {
        this.balanceRepository = balanceRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet.balance.changed", groupId = "paypulse-group")
    public void onBalanceChanged(String payload) {
        try {
            log.info("Analytics: received balance event: {}", payload);
            BalanceChangedEvent event = objectMapper.readValue(payload, BalanceChangedEvent.class);
            
            BalanceEvent analyticsEvent = new BalanceEvent(
                UUID.randomUUID().toString(),
                event.getUserId(),
                event.getType(),
                event.getDescription()
            );
            
            balanceRepository.save(analyticsEvent);
            log.info("Analytics: balance event consumed and saved: userId={}, type={}, id={}", 
                event.getUserId(), event.getType(), analyticsEvent.id());
        } catch (Exception e) {
            log.error("Failed to process balance event: payload={}", payload, e);
        }
    }
} 