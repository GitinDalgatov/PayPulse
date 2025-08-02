package com.paypulse.transaction.entity;

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
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID fromUserId;
    private UUID toUserId;
    private BigDecimal amount;
    private Instant timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    
    public enum TransactionStatus {
        PENDING,
        RESERVED,
        TRANSFERRED,
        COMPLETED,
        FAILED,
        COMPENSATED
    }
}