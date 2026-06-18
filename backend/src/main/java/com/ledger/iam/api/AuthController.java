package com.ledger.iam.api;

import com.ledger.iam.AuthService;
import com.ledger.iam.api.AuthDtos.LoginRequest;
import com.ledger.iam.api.AuthDtos.RefreshRequest;
import com.ledger.iam.api.AuthDtos.RegisterRequest;
import com.ledger.iam.api.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokens = authService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
