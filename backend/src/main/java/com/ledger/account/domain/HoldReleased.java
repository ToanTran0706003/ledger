package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một hold đã được nhả: trả lại phần available đã giữ. {@code amount} được mang theo để
 * projector cập nhật available mà không phải tra cứu hold gốc (event tự đủ thông tin).
 * Khi {@code reason} là CAPTURED, ngay sau event này là một {@link MoneyPosted} ghi nợ thật.
 */
public record HoldReleased(
        String accountId,
        String holdId,
        BigDecimal amount,
        HoldReleaseReason reason,
        Instant releasedAt)
        implements DomainEvent {
}
