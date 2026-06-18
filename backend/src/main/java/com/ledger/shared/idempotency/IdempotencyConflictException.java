package com.ledger.shared.idempotency;

/** Cùng Idempotency-Key nhưng payload khác lần trước -> lỗi phía client. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency-Key '%s' đã dùng cho một request khác".formatted(key));
    }
}
