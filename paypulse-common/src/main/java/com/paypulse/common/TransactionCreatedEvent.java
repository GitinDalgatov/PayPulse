package com.paypulse.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent(
        @NotNull UUID fromUserId,
        @NotNull UUID toUserId,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 1, max = 50) String type
) {}