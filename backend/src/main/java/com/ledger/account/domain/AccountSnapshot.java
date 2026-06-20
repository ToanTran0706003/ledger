package com.ledger.account.domain;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Trạng thái cô đọng của AccountAggregate để lưu snapshot. {@code holds} (holdId -> số tiền
 * đang giữ) phải nằm trong snapshot: nếu thiếu, khôi phục từ snapshot sẽ mất các hold và
 * invariant available = balance - Σ(hold) bị vỡ.
 */
public record AccountSnapshot(
        String accountId,
        String owner,
        AccountType type,
        String currency,
        AccountStatus status,
        BigDecimal balance,
        Map<String, BigDecimal> holds) {
}
