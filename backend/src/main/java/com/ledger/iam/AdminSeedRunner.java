package com.ledger.iam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seed tài khoản ADMIN khởi tạo khi ứng dụng khởi động (idempotent). Bật mặc định cho dev/demo;
 * TẮT trong profile prod và trong test. Khi tạo, cảnh báo đổi mật khẩu (xem ADR-0017).
 */
@Component
@ConditionalOnProperty(prefix = "ledger.admin.bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

    private final AdminSeedService adminSeed;
    private final String username;
    private final String password;

    public AdminSeedRunner(
            AdminSeedService adminSeed,
            @Value("${ledger.admin.bootstrap.username:admin}") String username,
            @Value("${ledger.admin.bootstrap.password:admin12345}") String password) {
        this.adminSeed = adminSeed;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminSeed.seedIfAbsent(username, password)) {
            log.warn("Đã tạo admin bootstrap '{}'. ĐỔI mật khẩu (LEDGER_ADMIN_PASSWORD) hoặc tắt "
                    + "(ledger.admin.bootstrap.enabled=false) ngoài môi trường dev/demo.", username);
        }
    }
}
