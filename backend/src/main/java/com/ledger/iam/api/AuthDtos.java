package com.ledger.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO cho các endpoint xác thực. */
public final class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String totpCode) {} // tuỳ chọn: chỉ cần khi tài khoản bật 2FA

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType) {}

    /** Trả về khi bắt đầu ghi danh 2FA: bí mật Base32 + URI otpauth để quét QR/nhập tay. */
    public record TwoFactorSetupResponse(String secret, String otpauthUri) {}

    public record TwoFactorCodeRequest(@NotBlank String code) {}

    public record TwoFactorStatusResponse(boolean enabled) {}

    private AuthDtos() {}
}
