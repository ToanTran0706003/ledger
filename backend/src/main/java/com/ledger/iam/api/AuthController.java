package com.ledger.iam.api;

import com.ledger.iam.AuthService;
import com.ledger.iam.TwoFactorService;
import com.ledger.iam.api.AuthDtos.LoginRequest;
import com.ledger.iam.api.AuthDtos.RefreshRequest;
import com.ledger.iam.api.AuthDtos.RegisterRequest;
import com.ledger.iam.api.AuthDtos.TokenResponse;
import com.ledger.iam.api.AuthDtos.TwoFactorCodeRequest;
import com.ledger.iam.api.AuthDtos.TwoFactorSetupResponse;
import com.ledger.iam.api.AuthDtos.TwoFactorStatusResponse;
import com.ledger.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TwoFactorService twoFactor;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, TwoFactorService twoFactor, CurrentUser currentUser) {
        this.authService = authService;
        this.twoFactor = twoFactor;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokens = authService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password(), request.totpCode());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** Đăng xuất: thu hồi mọi refresh token của user đang đăng nhập (cần access token hợp lệ). */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout(UUID.fromString(currentUser.requireUserId()));
    }

    // --- 2FA (TOTP): cần đăng nhập (access token) ---

    @GetMapping("/2fa/status")
    public TwoFactorStatusResponse twoFactorStatus() {
        return new TwoFactorStatusResponse(twoFactor.isEnabled(userId()));
    }

    /** Bắt đầu ghi danh 2FA: trả về bí mật + URI otpauth (chưa bật cho tới khi xác nhận mã). */
    @PostMapping("/2fa/setup")
    public TwoFactorSetupResponse twoFactorSetup() {
        return twoFactor.setup(userId());
    }

    @PostMapping("/2fa/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void twoFactorEnable(@Valid @RequestBody TwoFactorCodeRequest request) {
        twoFactor.enable(userId(), request.code());
    }

    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void twoFactorDisable(@Valid @RequestBody TwoFactorCodeRequest request) {
        twoFactor.disable(userId(), request.code());
    }

    private UUID userId() {
        return UUID.fromString(currentUser.requireUserId());
    }
}
