package com.paypulse.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;
    private BigDecimal amount;
    private String description;
    private Instant timestamp;
}