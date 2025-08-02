package com.paypulse.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.transaction.entity.OutboxEvent;
import com.paypulse.transaction.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${outbox.max-retries:3}")
    private int maxRetries;
    
    @Value("${outbox.batch-size:10}")
    private int batchSize;
    
    @Value("${outbox.cleanup.days:7}")
    private int cleanupDays;

    public OutboxProcessor(OutboxRepository outboxRepository,
                          KafkaTemplate<String, String> kafkaTemplate,
                          @Autowired ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Обработка событий из outbox каждые 5 секунд
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsForProcessing(OutboxEvent.EventStatus.PENDING);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            log.info("Processing {} outbox events", pendingEvents.size());
            
            for (OutboxEvent event : pendingEvents) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Error processing outbox events", e);
        }
    }

    /**
     * Повторная обработка неудачных событий каждые 30 секунд
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry(OutboxEvent.EventStatus.FAILED, maxRetries);
            
            if (failedEvents.isEmpty()) {
                return;
            }
            
            log.info("Retrying {} failed outbox events", failedEvents.size());
            
            for (OutboxEvent event : failedEvents) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Error retrying failed outbox events", e);
        }
    }

    /**
     * Очистка обработанных событий каждый день
     */
    @Scheduled(cron = "0 0 2 * * ?") 
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            Instant cutoffDate = Instant.now().minusSeconds(cleanupDays * 24 * 60 * 60);
            outboxRepository.deleteProcessedEventsOlderThan(cutoffDate);
            log.info("Cleaned up processed outbox events older than {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error cleaning up processed outbox events", e);
        }
    }

    /**
     * Обработка отдельного события
     */
    private void processEvent(OutboxEvent event) {
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

    /**
     * Обработка ошибки публикации
     */
    private void handlePublishError(OutboxEvent event, Throwable error) {
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

    /**
     * Метрики для мониторинга
     */
    public OutboxMetrics getMetrics() {
        return new OutboxMetrics(
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PENDING),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSING),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSED),
            outboxRepository.countByStatus(OutboxEvent.EventStatus.FAILED)
        );
    }

    /**
     * Класс для метрик outbox
     */
    public static class OutboxMetrics {
        private final long pending;
        private final long processing;
        private final long processed;
        private final long failed;

        public OutboxMetrics(long pending, long processing, long processed, long failed) {
            this.pending = pending;
            this.processing = processing;
            this.processed = processed;
            this.failed = failed;
        }

        public long getPending() { return pending; }
        public long getProcessing() { return processing; }
        public long getProcessed() { return processed; }
        public long getFailed() { return failed; }
    }
} 