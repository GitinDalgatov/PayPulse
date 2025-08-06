package com.paypulse.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public record TransactionEvent(
    @Id String id,
    UUID fromUserId,
    UUID toUserId,
    BigDecimal amount,
    String type
) {}