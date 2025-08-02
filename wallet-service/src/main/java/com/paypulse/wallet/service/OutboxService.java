package com.paypulse.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.wallet.entity.OutboxEvent;
import com.paypulse.wallet.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository,
                         @Autowired ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvent(String aggregateId, String aggregateType, String eventType, Object eventData) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .eventData(eventDataJson)
                    .createdAt(Instant.now())
                    .status(OutboxEvent.EventStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(outboxEvent);
            log.info("Saved outbox event: {} -> {} (aggregate: {})", eventType, outboxEvent.getId(), aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data for outbox: {}", eventType, e);
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }


    @Transactional
    public void saveBalanceEvent(String userId, Object eventData) {
        saveEvent(userId, "WALLET", "wallet.balance.changed", eventData);
    }
} 