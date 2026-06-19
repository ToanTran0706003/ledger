package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;

/**
 * Tài khoản bị đóng băng (kiểm soát rủi ro/gian lận). {@code reason} mô tả vì sao — do quản trị
 * viên hay do luật phát hiện gian lận. Khi FROZEN, mọi lệnh ghi nợ bị từ chối cho tới khi mở băng.
 */
public record AccountFrozen(String accountId, String reason, Instant frozenAt) implements DomainEvent {
}
