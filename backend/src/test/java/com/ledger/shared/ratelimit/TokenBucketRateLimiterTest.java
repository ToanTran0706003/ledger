package com.ledger.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    @Test
    void allows_up_to_capacity_then_rejects() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 0); // 2 token, không nạp lại

        assertThat(limiter.tryAcquire("ip-1")).isTrue();
        assertThat(limiter.tryAcquire("ip-1")).isTrue();
        assertThat(limiter.tryAcquire("ip-1")).isFalse(); // hết token
    }

    @Test
    void buckets_are_isolated_per_key() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 0);

        assertThat(limiter.tryAcquire("ip-1")).isTrue();
        assertThat(limiter.tryAcquire("ip-1")).isFalse();
        assertThat(limiter.tryAcquire("ip-2")).isTrue(); // key khác có xô riêng
    }
}
