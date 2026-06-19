package com.ledger.account.hold;

import java.math.BigDecimal;
import java.time.Instant;

/** Một hold như hiển thị cho người dùng. */
public record HoldView(
        String holdId,
        String accountId,
        BigDecimal amount,
        String status,
        String reason,
        Instant placedAt,
        Instant expiresAt,
        Instant releasedAt) {
}
