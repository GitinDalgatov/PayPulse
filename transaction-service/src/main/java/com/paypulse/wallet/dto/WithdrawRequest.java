package com.paypulse.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на списание средств с кошелька")
public record WithdrawRequest(
        @Schema(description = "UUID пользователя", example = "uuid1") @NotNull UUID userId,
        @Schema(description = "Сумма списания", example = "10.0") @NotNull @Positive BigDecimal amount
) {
}