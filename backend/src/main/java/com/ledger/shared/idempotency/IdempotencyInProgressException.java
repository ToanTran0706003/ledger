package com.ledger.shared.idempotency;

/** Một request cùng Idempotency-Key đang được xử lý -> tránh chạy song song. */
public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String key) {
        super("Idempotency-Key '%s' đang được xử lý".formatted(key));
    }
}
