package com.ledger.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.iam.api.AuthDtos.TokenResponse;
import com.ledger.iam.domain.RefreshTokenRepository;
import com.ledger.iam.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Refresh token rotation (single-use) + thu hồi khi logout (ADR-0021): mỗi refresh token chỉ dùng
 * được một lần — refresh cấp jti mới và vô hiệu jti cũ; logout thu hồi toàn bộ. Token lộ/tái dùng
 * không còn trong whitelist -> bị từ chối.
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE refresh_tokens");
    }

    @Test
    void refresh_rotates_and_old_token_is_single_use() {
        String username = "rot-" + UUID.randomUUID();
        TokenResponse first = authService.register(username, "password123");

        TokenResponse second = authService.refresh(first.refreshToken());
        assertThat(second.refreshToken()).isNotBlank();

        // Refresh token cũ KHÔNG dùng lại được sau khi đã xoay.
        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);

        // Refresh token mới vẫn hợp lệ.
        assertThat(authService.refresh(second.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void logout_revokes_all_refresh_tokens() {
        String username = "logout-" + UUID.randomUUID();
        TokenResponse tokens = authService.register(username, "password123");
        UUID userId = users.findByUsername(username).orElseThrow().getId();

        authService.logout(userId);

        assertThat(refreshTokens.findAll()).noneMatch(t -> t.getUserId().equals(userId));
        assertThatThrownBy(() -> authService.refresh(tokens.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
