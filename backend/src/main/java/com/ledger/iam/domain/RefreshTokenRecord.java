package com.ledger.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Một refresh token còn hiệu lực (whitelist). {@code id} = jti của token. Tồn tại hàng = token hợp
 * lệ; refresh tiêu thụ (xoá) hàng cũ và tạo hàng mới (rotation single-use); logout xoá theo user.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenRecord {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenRecord() {
        // JPA
    }

    public RefreshTokenRecord(UUID id, UUID userId, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
