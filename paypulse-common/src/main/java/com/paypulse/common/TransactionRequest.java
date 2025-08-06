package com.paypulse.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
    @NotNull(message = "ID отправителя обязателен")
    @JsonProperty("fromUserId")
    UUID fromUserId,
    
    @NotNull(message = "ID получателя обязателен")
    @JsonProperty("toUserId")
    UUID toUserId,
    
    @NotNull(message = "Сумма транзакции не может быть пустой")
    @Positive(message = "Сумма транзакции должна быть положительной")
    BigDecimal amount
) {} 