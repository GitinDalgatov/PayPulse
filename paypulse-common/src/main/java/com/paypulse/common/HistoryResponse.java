package com.paypulse.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID uuid,
        BigDecimal amount,
        String description,
        Instant timestamp
) {
}