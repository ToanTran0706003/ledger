package com.ledger.shared.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình rate limiting: hai xô token-bucket (auth chặt, ghi rộng), gắn interceptor cho mọi route.
 * Ngưỡng cấu hình qua {@code ledger.rate-limit.*}; bật mặc định, tắt trong test để không nhiễu.
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final boolean enabled;
    private final int authCapacity;
    private final double authRefillPerMinute;
    private final int writeCapacity;
    private final double writeRefillPerMinute;

    public RateLimitConfig(
            @Value("${ledger.rate-limit.enabled:true}") boolean enabled,
            @Value("${ledger.rate-limit.auth.capacity:10}") int authCapacity,
            @Value("${ledger.rate-limit.auth.refill-per-minute:10}") double authRefillPerMinute,
            @Value("${ledger.rate-limit.write.capacity:60}") int writeCapacity,
            @Value("${ledger.rate-limit.write.refill-per-minute:60}") double writeRefillPerMinute) {
        this.enabled = enabled;
        this.authCapacity = authCapacity;
        this.authRefillPerMinute = authRefillPerMinute;
        this.writeCapacity = writeCapacity;
        this.writeRefillPerMinute = writeRefillPerMinute;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        TokenBucketRateLimiter authLimiter = new TokenBucketRateLimiter(authCapacity, authRefillPerMinute / 60.0);
        TokenBucketRateLimiter writeLimiter = new TokenBucketRateLimiter(writeCapacity, writeRefillPerMinute / 60.0);
        registry.addInterceptor(new RateLimitInterceptor(authLimiter, writeLimiter, enabled));
    }
}
