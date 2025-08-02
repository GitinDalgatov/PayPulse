package com.paypulse.common;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoryResponse(
    BigDecimal amount,
    String description,
    Instant timestamp
) {} 