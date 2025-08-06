package com.paypulse.wallet.repository;

import com.paypulse.wallet.entity.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoryRepository extends JpaRepository<TransactionHistory, Long> {

    Page<TransactionHistory> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);
}