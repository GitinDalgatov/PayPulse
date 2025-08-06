package com.paypulse.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "Сумма пополнения не может быть пустой")
        @Positive(message = "Сумма пополнения должна быть положительной")
        BigDecimal amount,

        @NotBlank(message = "Описание операции обязательно для заполнения")
        String description
) {
}