package com.ledger.account.api;

import com.ledger.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(
        @NotBlank String owner,
        @NotNull AccountType type) {
}
