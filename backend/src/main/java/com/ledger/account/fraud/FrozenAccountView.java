package com.ledger.account.fraud;

/** Một tài khoản đang bị đóng băng và lý do (cho màn giám sát của ADMIN). */
public record FrozenAccountView(String accountId, String owner, String freezeReason) {
}
