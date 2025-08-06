package com.paypulse.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.transaction.entity.OutboxEvent;
import com.paypulse.transaction.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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
    public void saveTransactionEvent(String transactionId, Object eventData) {
        saveEvent(transactionId, "TRANSACTION", "transaction.created", eventData);
    }

    @Transactional
    public void saveBalanceEvent(String userId, Object eventData) {
        saveEvent(userId, "WALLET", "wallet.balance.changed", eventData);
    }
}