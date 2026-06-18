package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;

/** Tài khoản đã được mở. */
public record AccountOpened(
        String accountId,
        String owner,
        AccountType type,
        Instant openedAt) implements DomainEvent {
}
