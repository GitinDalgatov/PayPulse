package com.paypulse.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID fromUserId,
    UUID toUserId,
    BigDecimal amount,
    Instant timestamp
) {} 