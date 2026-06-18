package com.ledger.account.query;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryView(
        String txId,
        String direction,
        BigDecimal amount,
        String counterparty,
        BigDecimal balanceAfter,
        String movementType,
        Instant occurredAt) {
}
