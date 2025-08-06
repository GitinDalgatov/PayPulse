package com.paypulse.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypulse.wallet.entity.OutboxEvent;
import com.paypulse.wallet.repository.OutboxRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
        List<OutboxEvent> pending = outboxRepository.findPendingEventsForProcessing(OutboxEvent.EventStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        int count = Math.min(pending.size(), batchSize);
        List<OutboxEvent> batch = pending.subList(0, count);
        log.info("Processing {} outbox events (batch size {})", count, batchSize);
        batch.forEach(this::processEvent);
    }

    @Scheduled(fixedRate = 30000)
    public void retryFailedEvents() {
        List<OutboxEvent> failed = outboxRepository.findFailedEventsForRetry(OutboxEvent.EventStatus.FAILED, maxRetries);
        if (failed.isEmpty()) {
            return;
        }
        int count = Math.min(failed.size(), batchSize);
        List<OutboxEvent> batch = failed.subList(0, count);
        log.info("Retrying {} failed outbox events (batch size {})", count, batchSize);
        batch.forEach(this::processEvent);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupProcessedEvents() {
        Instant cutoff = Instant.now().minusSeconds(cleanupDays * 86400L);
        int deleted = outboxRepository.deleteProcessedEventsOlderThan(cutoff);
        log.info("Cleaned up {} processed outbox events older than {}", deleted, cutoff);
    }

    private void processEvent(OutboxEvent event) {
        event.setStatus(OutboxEvent.EventStatus.PROCESSING);
        outboxRepository.save(event);
        kafkaTemplate.send(event.getEventType(), event.getEventData())
                .whenComplete((res, err) -> {
                    if (err != null) {
                        handleError(event, err);
                    } else {
                        event.setStatus(OutboxEvent.EventStatus.PROCESSED);
                        event.setProcessedAt(Instant.now());
                        event.setErrorMessage(null);
                        outboxRepository.save(event);
                        log.info("Processed outbox event {} -> {}", event.getId(), event.getEventType());
                    }
                });
    }

    private void handleError(OutboxEvent event, Throwable err) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(err.getMessage());
        if (event.getRetryCount() >= maxRetries) {
            event.setStatus(OutboxEvent.EventStatus.FAILED);
            log.error("Outbox event {} failed permanently after {} retries", event.getId(), maxRetries, err);
        } else {
            event.setStatus(OutboxEvent.EventStatus.PENDING);
            log.warn("Outbox event {} failed, will retry {}/{}", event.getId(), event.getRetryCount(), maxRetries);
        }
        outboxRepository.save(event);
    }

    @Getter
    @RequiredArgsConstructor
    public static class OutboxMetrics {
        private final long pending;
        private final long processing;
        private final long processed;
        private final long failed;

    }

    public OutboxMetrics getMetrics() {
        return new OutboxMetrics(
                outboxRepository.countByStatus(OutboxEvent.EventStatus.PENDING),
                outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSING),
                outboxRepository.countByStatus(OutboxEvent.EventStatus.PROCESSED),
                outboxRepository.countByStatus(OutboxEvent.EventStatus.FAILED)
        );
    }
}