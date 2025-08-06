package com.paypulse.transaction.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.transaction.entity.Transaction;
import com.paypulse.common.TransactionCreatedEvent;
import com.paypulse.common.KafkaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Transaction transaction) {
        try {
            TransactionCreatedEvent event = new TransactionCreatedEvent(
                    transaction.getFromUserId(),
                    transaction.getToUserId(),
                    transaction.getAmount(),
                    "TRANSACTION"
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("transaction.created", json);
            log.info("Transaction created event published: from={}, to={}, amount={}",
                    transaction.getFromUserId(), transaction.getToUserId(), transaction.getAmount());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction created event: transactionId={}",
                    transaction.getId(), e);
            throw new KafkaException("Failed to serialize transaction created event", e);
        }
    }
}