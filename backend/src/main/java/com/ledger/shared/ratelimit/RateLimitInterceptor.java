package com.ledger.shared.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn lạm dụng: giới hạn tần suất các request GHI theo IP. {@code /auth/**} (đăng nhập/đăng ký,
 * permitAll) dùng giới hạn chặt để **chống dò mật khẩu** — đây là tác dụng chính. Các lệnh ghi khác
 * đã qua xác thực (Spring Security từ chối 401 trước khi tới đây nếu chưa đăng nhập) dùng giới hạn
 * rộng hơn để chặn lạm dụng theo phiên. Request đọc (GET/HEAD/OPTIONS) không bị giới hạn.
 *
 * Key là {@code request.getRemoteAddr()} — IP peer thật, KHÔNG tin header X-Forwarded-For do client
 * tự đặt (sẽ giả mạo được để vượt mặt limiter). Khi chạy sau proxy tin cậy, bật
 * {@code server.forward-headers-strategy} để Spring tự cập nhật getRemoteAddr từ header chuẩn.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter authLimiter;
    private final TokenBucketRateLimiter writeLimiter;
    private final boolean enabled;

    public RateLimitInterceptor(
            TokenBucketRateLimiter authLimiter, TokenBucketRateLimiter writeLimiter, boolean enabled) {
        this.authLimiter = authLimiter;
        this.writeLimiter = writeLimiter;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || isRead(request.getMethod())) {
            return true;
        }
        String ip = request.getRemoteAddr();
        boolean allowed = request.getRequestURI().startsWith("/auth/")
                ? authLimiter.tryAcquire("auth:" + ip)
                : writeLimiter.tryAcquire("write:" + ip);
        if (!allowed) {
            throw new RateLimitExceededException();
        }
        return true;
    }

    private static boolean isRead(String method) {
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method);
    }
}
