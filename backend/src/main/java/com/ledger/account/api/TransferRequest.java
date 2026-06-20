package com.ledger.account.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromAccountId,
        @NotBlank String toAccountId,
        @NotNull @Positive @Digits(integer = 16, fraction = 2) BigDecimal amount) {
}
