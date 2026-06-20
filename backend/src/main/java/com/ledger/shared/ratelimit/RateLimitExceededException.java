package com.ledger.shared.ratelimit;

/** Ném khi vượt giới hạn tần suất request; map sang HTTP 429. */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Quá nhiều yêu cầu, vui lòng thử lại sau ít phút");
    }
}
