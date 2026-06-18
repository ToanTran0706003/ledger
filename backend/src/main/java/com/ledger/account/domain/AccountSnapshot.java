package com.ledger.account.domain;

import java.math.BigDecimal;

/** Trạng thái cô đọng của AccountAggregate để lưu snapshot. */
public record AccountSnapshot(
        String accountId,
        String owner,
        AccountType type,
        AccountStatus status,
        BigDecimal balance) {
}
