package com.ledger.account.domain;

/**
 * Loại di chuyển tiền sinh ra posting. GENESIS là bút toán "khai sinh" tiền cho
 * SYSTEM_VAULT — vế duy nhất không cần đối ứng (xem ADR-0005).
 */
public enum MovementType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    GENESIS,
    REVERSAL,
    INTEREST
}
