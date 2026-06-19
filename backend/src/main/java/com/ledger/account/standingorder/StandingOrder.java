package com.ledger.account.standingorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Lệnh chuyển tiền lặp lại theo chu kỳ. Dùng JPA (trạng thái CRUD, không event-sourced). */
@Entity
@Table(name = "standing_orders")
public class StandingOrder {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    @Column(name = "from_account_id", nullable = false)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private String toAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "interval_seconds", nullable = false)
    private long intervalSeconds;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StandingOrder() {
        // JPA
    }

    public StandingOrder(
            UUID id,
            String ownerUserId,
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            long intervalSeconds,
            Instant nextRunAt,
            Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.intervalSeconds = intervalSeconds;
        this.nextRunAt = nextRunAt;
        this.active = true;
        this.createdAt = createdAt;
    }

    /** Dời thời điểm chạy kế tiếp lên một chu kỳ (sau khi đã thực thi hoặc bỏ qua một lần). */
    public void advance() {
        this.nextRunAt = this.nextRunAt.plusSeconds(intervalSeconds);
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public boolean isActive() {
        return active;
    }
}
