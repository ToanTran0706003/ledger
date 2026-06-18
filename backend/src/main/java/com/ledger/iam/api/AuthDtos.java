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
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType) {}

    private AuthDtos() {}
}
