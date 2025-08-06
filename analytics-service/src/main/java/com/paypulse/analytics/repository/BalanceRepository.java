package com.paypulse.analytics.repository;

import com.paypulse.analytics.entity.BalanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceRepository extends JpaRepository<BalanceEvent, String> {

    @Query("SELECT b FROM BalanceEvent b WHERE b.userId = :userId")
    List<BalanceEvent> findByUserId(UUID userId);
} 