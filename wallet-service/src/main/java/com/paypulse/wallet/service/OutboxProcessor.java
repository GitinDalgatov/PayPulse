package com.paypulse.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.wallet.entity.OutboxEvent;
import com.paypulse.wallet.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxProcessor {
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${outbox.max-retries:3}")
    private int maxRetries;
    
    @Value("${outbox.batch-size:10}")
    private int batchSize;
    
    @Value("${outbox.cleanup.days:7}")
    private int cleanupDays;

    @Scheduled(fixedRate = 5000)
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsForProcessing(OutboxEvent.EventStatus.PENDING);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            int eventsToProcess = Math.min(pendingEvents.size(), batchSize);
            List<OutboxEvent> eventsToProcessList = pendingEvents.subList(0, eventsToProcess);
            
            log.info("Processing {} outbox events (batch size: {})", eventsToProcess, batchSize);
            
            for (OutboxEvent event : eventsToProcessList) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Error processing outbox events", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry(OutboxEvent.EventStatus.FAILED, maxRetries);
            
            if (failedEvents.isEmpty()) {
                return;
            }
            
            int eventsToRetry = Math.min(failedEvents.size(), batchSize);
            List<OutboxEvent> eventsToRetryList = failedEvents.subList(0, eventsToRetry);
            
            log.info("Retrying {} failed outbox events (batch size: {})", eventsToRetry, batchSize);
            
            for (OutboxEvent event : eventsToRetryList) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Error retrying failed outbox events", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") 
    public void cleanupProcessedEvents() {
        try {
            Instant cutoffDate = Instant.now().minusSeconds(cleanupDays * 24 * 60 * 60);
            outboxRepository.deleteProcessedEventsOlderThan(cutoffDate);
            log.info("Cleaned up processed outbox events older than {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error cleaning up processed outbox events", e);
        }
    }

    private void processEvent(OutboxEvent event) {
        if (event == null) {
            log.error("Cannot process null event");
            return;
        }
        
        try {
            event.setStatus(OutboxEvent.EventStatus.PROCESSING);
            outboxRepository.save(event);
            kafkaTemplate.send(event.getEventType(), event.getEventData())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        
                        handlePublishError(event, throwable);
                    } else {
                        
                        event.setStatus(OutboxEvent.EventStatus.PROCESSED);
                        event.setProcessedAt(Instant.now());
                        event.setErrorMessage(null);
                        outboxRepository.save(event);
                        
                        log.info("Successfully processed outbox event: {} -> {}", event.getId(), event.getEventType());
                    }
                });
                
        } catch (Exception e) {
            handlePublishError(event, e);
        }
    }

    private void handlePublishError(OutboxEvent event, Throwable error) {
        if (event == null || error == null) {
            log.error("Cannot handle publish error: event={}, error={}", event, error);
            return;
        }
        
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(error.getMessage());
        
        if (event.getRetryCount() >= maxRetries) {
            event.setStatus(OutboxEvent.EventStatus.FAILED);
            log.error("Outbox event failed permanently after {} retries: {}", maxRetries, event.getId(), error);
        } else {
            event.setStatus(OutboxEvent.EventStatus.PENDING);
            log.warn("Outbox event failed, will retry (attempt {}/{}): {}", 
                event.getRetryCount(), maxRetries, event.getId());
        }
        
        outboxRepository.save(event);
    }

    public OutboxMetrics getMetrics() {
        return new OutboxMetrics(
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PENDING),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSING),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSED),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.FAILED)
        );
    }

    public record OutboxMetrics(
        long pending,
        long processing,
        long processed,
        long failed
    ) {}
} 