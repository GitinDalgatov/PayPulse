package com.paypulse.transaction.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.transaction.entity.Transaction;
import com.paypulse.common.TransactionCreatedEvent;
import com.paypulse.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransactionProducer(KafkaTemplate<String, String> kafkaTemplate,
                             @Autowired ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

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