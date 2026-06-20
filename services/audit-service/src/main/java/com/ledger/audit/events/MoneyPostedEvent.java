package com.ledger.audit.events;

import java.math.BigDecimal;
import java.time.Instant;

public record MoneyPostedEvent(
        String accountId,
        String txId,
        Direction direction,
        BigDecimal amount,
        String movementType,
        String counterpartyAccountId,
        Instant postedAt,
        String reversalOfTxId
) {

    public enum Direction {
        CREDIT,
        DEBIT
    }
}
