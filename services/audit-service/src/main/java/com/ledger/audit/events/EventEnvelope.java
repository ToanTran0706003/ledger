package com.ledger.audit.events;

public record EventEnvelope(
        String eventType,
        String payload
) {
}
