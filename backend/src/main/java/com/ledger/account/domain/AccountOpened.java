package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;

/** Tài khoản đã được mở. {@code currency} có thể null ở event cũ (trước đa tiền tệ) -> coi như VND. */
public record AccountOpened(
        String accountId,
        String owner,
        AccountType type,
        String currency,
        Instant openedAt) implements DomainEvent {
}
