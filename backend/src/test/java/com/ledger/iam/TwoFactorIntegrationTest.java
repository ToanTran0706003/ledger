package com.ledger.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.iam.api.AuthDtos.TwoFactorSetupResponse;
import com.ledger.iam.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 2FA (TOTP): sau khi bật, đăng nhập đúng mật khẩu nhưng thiếu/sai mã -> bị chặn; đúng mã -> cấp
 * token. Tắt 2FA quay lại đăng nhập một lớp (ADR-0021).
 */
@SpringBootTest
@ActiveProfiles("test")
class TwoFactorIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TwoFactorService twoFactor;

    @Autowired
    private TotpService totp;

    @Autowired
    private UserRepository users;

    private String currentCode(String secret) {
        return totp.codeAt(TotpService.base32Decode(secret), Instant.now().getEpochSecond() / 30);
    }

    @Test
    void enabling_2fa_then_login_requires_valid_code() {
        String username = "tfa-" + UUID.randomUUID();
        authService.register(username, "password123");
        UUID id = users.findByUsername(username).orElseThrow().getId();

        // Trước khi bật: đăng nhập một lớp hoạt động.
        assertThat(authService.login(username, "password123").accessToken()).isNotBlank();

        // Ghi danh + bật bằng mã hợp lệ.
        TwoFactorSetupResponse setup = twoFactor.setup(id);
        twoFactor.enable(id, currentCode(setup.secret()));

        // Thiếu mã -> cần 2FA.
        assertThatThrownBy(() -> authService.login(username, "password123"))
                .isInstanceOf(TwoFactorRequiredException.class);
        // Sai mã -> từ chối.
        assertThatThrownBy(() -> authService.login(username, "password123", "000000"))
                .isInstanceOf(InvalidTwoFactorCodeException.class);
        // Đúng mã -> cấp token.
        assertThat(authService.login(username, "password123", currentCode(setup.secret())).accessToken())
                .isNotBlank();
    }

    @Test
    void enabling_requires_a_valid_confirmation_code() {
        String username = "tfa-bad-" + UUID.randomUUID();
        authService.register(username, "password123");
        UUID id = users.findByUsername(username).orElseThrow().getId();
        twoFactor.setup(id);

        assertThatThrownBy(() -> twoFactor.enable(id, "000000"))
                .isInstanceOf(InvalidTwoFactorCodeException.class);
        assertThat(twoFactor.isEnabled(id)).isFalse();
    }

    @Test
    void disabling_2fa_returns_to_single_factor() {
        String username = "tfa-off-" + UUID.randomUUID();
        authService.register(username, "password123");
        UUID id = users.findByUsername(username).orElseThrow().getId();
        TwoFactorSetupResponse setup = twoFactor.setup(id);
        twoFactor.enable(id, currentCode(setup.secret()));

        twoFactor.disable(id, currentCode(setup.secret()));

        assertThat(twoFactor.isEnabled(id)).isFalse();
        assertThat(authService.login(username, "password123").accessToken()).isNotBlank();
    }
}
