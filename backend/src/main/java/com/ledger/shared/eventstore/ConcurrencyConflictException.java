package com.ledger.shared.eventstore;

/**
 * Ném ra khi hai request đồng thời cùng cố ghi một version cho một aggregate.
 * Tầng trên có thể bắt và retry (đọc lại version mới, validate lại) — xem 04-security mục 4.
 */
public class ConcurrencyConflictException extends RuntimeException {

    public ConcurrencyConflictException(String aggregateId, int conflictingVersion) {
        super("Conflict ghi version %d cho aggregate %s".formatted(conflictingVersion, aggregateId));
    }
}
