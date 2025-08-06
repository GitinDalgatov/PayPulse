package com.paypulse.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public record BalanceEvent(
    @Id String id,
    UUID userId,
    String type,
    String description
) {} 