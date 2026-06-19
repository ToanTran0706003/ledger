package com.ledger.account.domain;

/** Vì sao một hold được nhả: do người dùng, do hết hạn, hay do đã capture thành bút toán. */
public enum HoldReleaseReason {
    MANUAL,
    EXPIRED,
    CAPTURED
}
