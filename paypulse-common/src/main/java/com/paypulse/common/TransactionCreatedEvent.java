package com.paypulse.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public class TransactionCreatedEvent {
    @NotNull
    private UUID fromUserId;
    @NotNull
    private UUID toUserId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    @Size(min = 1, max = 50)
    private String type;
    
    public TransactionCreatedEvent() {}
    public TransactionCreatedEvent(UUID fromUserId, UUID toUserId, BigDecimal amount, String type) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
        this.type = type;
    }
    public UUID getFromUserId() { return fromUserId; }
    public void setFromUserId(UUID fromUserId) { this.fromUserId = fromUserId; }
    public UUID getToUserId() { return toUserId; }
    public void setToUserId(UUID toUserId) { this.toUserId = toUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
} 