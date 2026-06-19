package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Đã đặt giữ (reserve) một phần số dư: giảm số dư khả dụng (available) nhưng chưa rời
 * tài khoản. Tiền chỉ thực sự đi khi hold được capture; nếu không sẽ được nhả (release)
 * hoặc tự nhả khi tới {@code expiresAt}.
 */
public record HoldPlaced(
        String accountId,
        String holdId,
        BigDecimal amount,
        Instant placedAt,
        Instant expiresAt)
        implements DomainEvent {
}
