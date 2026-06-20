package com.ledger.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Người dùng đăng nhập. Dùng JPA (CRUD thường), khác event store JDBC (ADR-0009). */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Bí mật TOTP (Base32) và cờ đã bật 2FA. Secret tồn tại nhưng chưa enabled = đang ghi danh,
    // chờ xác nhận bằng một mã hợp lệ trước khi bật (xem TwoFactorService).
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled;

    protected UserAccount() {
        // JPA
    }

    public UserAccount(UUID id, String username, String passwordHash, Role role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    /** Bắt đầu ghi danh 2FA: lưu secret nhưng CHƯA bật (chờ xác nhận bằng mã hợp lệ). */
    public void startTotpEnrollment(String secret) {
        this.totpSecret = secret;
        this.totpEnabled = false;
    }

    public void confirmTotp() {
        this.totpEnabled = true;
    }

    public void disableTotp() {
        this.totpSecret = null;
        this.totpEnabled = false;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }
}
