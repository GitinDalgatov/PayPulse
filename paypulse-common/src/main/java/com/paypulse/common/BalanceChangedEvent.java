package com.paypulse.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BalanceChangedEvent(
        @NotNull
        UUID userId,
        @NotNull
        @Size(min = 1, max = 50)
        String type,
        @Size(max = 255)
        String description
) {}