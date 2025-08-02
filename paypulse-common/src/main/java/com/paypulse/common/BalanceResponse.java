package com.paypulse.common;

import java.math.BigDecimal;

public record BalanceResponse(
    BigDecimal balance
) {} 