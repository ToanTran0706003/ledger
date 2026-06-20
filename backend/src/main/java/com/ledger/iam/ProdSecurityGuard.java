package com.ledger.iam;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Chặn khởi động ở profile prod nếu bí mật bảo mật vẫn là giá trị mặc định dev (fail-fast). Tránh
 * tình huống deploy quên đặt biến môi trường rồi chạy bằng secret/mật khẩu công khai trong repo
 * (audit #2, #3 — xem ADR-0021). Chỉ active khi {@code SPRING_PROFILES_ACTIVE=prod}.
 */
@Configuration
@Profile("prod")
public class ProdSecurityGuard {

    static final String DEV_JWT_SECRET = "dev-only-secret-please-change-32bytes-minimum-0123456789";
    static final String DEV_DB_PASSWORD = "ledger";

    private final String jwtSecret;
    private final String dbPassword;

    public ProdSecurityGuard(
            @Value("${ledger.security.jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password}") String dbPassword) {
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
    }

    @PostConstruct
    void verify() {
        if (jwtSecret == null || jwtSecret.isBlank() || DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "LEDGER_JWT_SECRET chưa đặt cho prod (đang dùng secret mặc định dev nằm trong repo). "
                            + "Đặt một secret ngẫu nhiên >= 32 byte qua biến môi trường.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("LEDGER_JWT_SECRET phải >= 32 byte cho HS256.");
        }
        if (DEV_DB_PASSWORD.equals(dbPassword)) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_PASSWORD vẫn là mật khẩu dev mặc định ở prod. Đặt mật khẩu mạnh qua biến môi trường.");
        }
    }
}
