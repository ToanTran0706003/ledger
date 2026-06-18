package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một bút toán (posting) lên một tài khoản — đơn vị cơ bản của ghi sổ kép.
 * Mỗi lần di chuyển tiền sinh hai MoneyPosted (một CREDIT, một DEBIT) cùng txId
 * trên hai tài khoản (ADR-0005). counterpartyAccountId là tài khoản đối ứng
 * (null với bút toán GENESIS).
 */
public record MoneyPosted(
        String accountId,
        String txId,
        Direction direction,
        BigDecimal amount,
        MovementType movementType,
        String counterpartyAccountId,
        Instant postedAt) implements DomainEvent {
}
