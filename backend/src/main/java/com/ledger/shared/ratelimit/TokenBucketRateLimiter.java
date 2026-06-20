package com.ledger.shared.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter token-bucket trong bộ nhớ, theo từng key (vd IP). Mỗi key có một "xô" {@code capacity}
 * token, nạp lại đều theo thời gian ({@code refillPerSecond}). Mỗi request lấy 1 token; hết token →
 * từ chối. Đủ cho một node (modular monolith); đa node cần backend chia sẻ (Redis) — xem ADR-0018.
 *
 * Thread-safe: toàn bộ refill + lấy token chạy nguyên tử trong {@code compute} của ConcurrentHashMap.
 */
public class TokenBucketRateLimiter {

    // Backstop chống map phình vô hạn (key = IP peer thật). Đạt mức này thì xóa sạch (reset nhẹ,
    // fail-safe). Cardinality lớn tới mức này cần rate limit phân tán (Redis) — xem ADR-0018.
    private static final int MAX_KEYS = 100_000;

    private final double capacity;
    private final double refillPerSecond;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    /** Lấy 1 token cho {@code key}; trả về false nếu đã hết (nên từ chối request). */
    public boolean tryAcquire(String key) {
        if (buckets.size() >= MAX_KEYS && !buckets.containsKey(key)) {
            buckets.clear();
        }
        boolean[] acquired = {false};
        buckets.compute(key, (k, existing) -> {
            long now = System.nanoTime();
            Bucket bucket = existing == null ? new Bucket(capacity, now) : existing;
            bucket.refill(capacity, refillPerSecond, now);
            acquired[0] = bucket.tryTake();
            return bucket;
        });
        return acquired[0];
    }

    private static final class Bucket {
        private double tokens;
        private long lastNanos;

        Bucket(double tokens, long lastNanos) {
            this.tokens = tokens;
            this.lastNanos = lastNanos;
        }

        void refill(double capacity, double refillPerSecond, long now) {
            double elapsedSeconds = (now - lastNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastNanos = now;
        }

        boolean tryTake() {
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
