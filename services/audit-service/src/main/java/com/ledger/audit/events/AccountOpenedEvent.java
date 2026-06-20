package com.ledger.audit.events;

import java.time.Instant;

public record AccountOpenedEvent(
        String accountId,
        String owner,
        String type,
        String currency,
        Instant openedAt
) {
}
