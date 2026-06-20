package com.ledger.account.query;

import java.math.BigDecimal;

public record AccountBalanceView(
        String accountId,
        String owner,
        String type,
        String currency,
        BigDecimal balance,
        BigDecimal available,
        String status,
        String freezeReason) {
}
