package com.paypulse.common;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String type,
    String message,
    Instant timestamp
) {} 