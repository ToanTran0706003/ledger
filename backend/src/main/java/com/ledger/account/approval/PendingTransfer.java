package com.ledger.account.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Yêu cầu chuyển tiền vượt ngưỡng đang CHỜ DUYỆT (maker-checker). Trạng thái CRUD -> JPA, không
 * event-sourced: chỉ khi được duyệt mới phát giao dịch thật vào event store. Lưu cả người tạo
 * (maker) và người quyết định (checker) để kiểm toán + thực thi nguyên tắc bốn-mắt.
 */
@Entity
@Table(name = "pending_transfers")
public class PendingTransfer {

    @Id
    private UUID id;

    @Column(name = "maker_user_id", nullable = false)
    private String makerUserId;

    @Column(name = "from_account_id", nullable = false)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private String toAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "tx_id")
    private String txId;

    protected PendingTransfer() {
        // JPA
    }

    public PendingTransfer(
            UUID id, String makerUserId, String fromAccountId, String toAccountId, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.makerUserId = makerUserId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = ApprovalStatus.PENDING;
        this.createdAt = createdAt;
    }

    /** Đánh dấu đã duyệt + ghi txId của giao dịch thật vừa thực thi (validation ở service). */
    public void markApproved(String approverUserId, String txId, Instant at) {
        this.status = ApprovalStatus.APPROVED;
        this.decidedBy = approverUserId;
        this.decidedAt = at;
        this.txId = txId;
    }

    public void markRejected(String approverUserId, String reason, Instant at) {
        this.status = ApprovalStatus.REJECTED;
        this.decidedBy = approverUserId;
        this.decidedAt = at;
        this.decisionReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getMakerUserId() {
        return makerUserId;
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

    public ApprovalStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTxId() {
        return txId;
    }
}
