package com.paypulse.analytics.repository;

import com.paypulse.analytics.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEvent, String> {

    @Query("SELECT COUNT(t) FROM TransactionEvent t WHERE t.fromUserId = :userId OR t.toUserId = :userId")
    long countByFromUserIdOrToUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEvent t WHERE t.fromUserId = :userId OR t.toUserId = :userId")
    BigDecimal sumAmountByUserId(UUID userId);

    @Query("SELECT t FROM TransactionEvent t WHERE t.fromUserId = :fromUserId OR t.toUserId = :toUserId")
    List<TransactionEvent> findByFromUserIdOrToUserId(UUID fromUserId, UUID toUserId);
}