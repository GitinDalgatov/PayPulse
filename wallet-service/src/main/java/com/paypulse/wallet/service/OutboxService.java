package com.paypulse.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.wallet.entity.OutboxEvent;
import com.paypulse.wallet.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(String aggregateId, String aggregateType, String eventType, Object eventData) {
        try {
            String json = objectMapper.writeValueAsString(eventData);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .eventData(json)
                    .createdAt(Instant.now())
                    .status(OutboxEvent.EventStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
            log.info("Saved outbox event: {} -> {} (aggregate: {})", eventType, event.getId(), aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event '{}' data", eventType, e);
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }

    @Transactional
    public void saveBalanceEvent(String userId, Object eventData) {
        saveEvent(userId, "WALLET", "wallet.balance.changed", eventData);
    }
}