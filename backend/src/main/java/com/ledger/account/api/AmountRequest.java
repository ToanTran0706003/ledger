package com.ledger.account.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Body cho nạp/rút: chỉ cần số tiền dương. */
public record AmountRequest(
        @NotNull @Positive BigDecimal amount) {
}
