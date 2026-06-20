package com.ledger.account.fx;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Body quy đổi: từ tài khoản (tiền tệ A) sang tài khoản (tiền tệ B), số tiền theo tiền tệ A. */
public record ExchangeRequest(
        @NotBlank String fromAccountId, @NotBlank String toAccountId, @NotNull @Positive BigDecimal amount) {
}
