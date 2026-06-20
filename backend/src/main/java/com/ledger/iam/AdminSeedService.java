package com.ledger.iam;

import com.ledger.iam.domain.Role;
import com.ledger.iam.domain.UserAccount;
import com.ledger.iam.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo sẵn một tài khoản ADMIN khởi tạo (bootstrap) để vận hành/kiểm toán có thể đăng nhập ngay.
 * Idempotent: chỉ tạo nếu username chưa tồn tại. Mật khẩu băm BCrypt như mọi user khác.
 * Bật mặc định cho dev/demo, tắt ở prod — đổi/tắt trong production (xem ADR-0017).
 */
@Service
public class AdminSeedService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminSeedService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Tạo admin nếu chưa có; trả về true nếu vừa tạo, false nếu đã tồn tại. */
    @Transactional
    public boolean seedIfAbsent(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            return false;
        }
        UserAccount admin = new UserAccount(
                UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), Role.ADMIN, Instant.now());
        users.save(admin);
        return true;
    }
}
