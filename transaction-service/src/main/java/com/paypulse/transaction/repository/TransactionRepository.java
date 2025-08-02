package com.paypulse.transaction.repository;

import com.paypulse.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByFromUserIdOrToUserIdOrderByTimestampDesc(UUID fromUserId, UUID toUserId);
}