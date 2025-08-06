package com.paypulse.transaction.repository;

import com.paypulse.transaction.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsForProcessing(@Param("status") OutboxEvent.EventStatus status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findFailedEventsForRetry(@Param("status") OutboxEvent.EventStatus status, @Param("maxRetries") int maxRetries);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PROCESSED' AND e.processedAt < :cutoffDate")
    void deleteProcessedEventsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    long countByStatus(OutboxEvent.EventStatus status);
}