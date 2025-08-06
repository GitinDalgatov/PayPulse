package com.paypulse.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.analytics.entity.TransactionEvent;
import com.paypulse.analytics.repository.TransactionRepository;
import com.paypulse.common.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction.created", groupId = "paypulse-group")
    public void onTransaction(String payload) {
        try {
            log.info("Analytics: received transaction event: {}", payload);
            TransactionCreatedEvent event = objectMapper.readValue(payload, TransactionCreatedEvent.class);
            
            TransactionEvent analyticsEvent = new TransactionEvent(
                UUID.randomUUID().toString(),
                event.fromUserId(),
                event.toUserId(),
                event.amount(),
                event.type()
            );
            
            transactionRepository.save(analyticsEvent);
            log.info("Analytics: transaction event consumed and saved: from={}, to={}, amount={}, id={}", 
                event.fromUserId(), event.toUserId(), event.amount(), analyticsEvent.id());
        } catch (Exception e) {
            log.error("Failed to process transaction event: payload={}", payload, e);
        }
    }
} 