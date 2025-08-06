package com.paypulse.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawRequest(
    @NotNull(message = "Сумма снятия не может быть пустой")
    @Positive(message = "Сумма снятия должна быть положительной")
    BigDecimal amount,
    
    @NotBlank(message = "Описание операции обязательно для заполнения")
    String description
) {} 