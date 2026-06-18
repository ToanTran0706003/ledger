package com.ledger.account.domain;

import java.math.BigDecimal;

/** Ném ra khi một tài khoản CUSTOMER bị debit vượt quá số dư khả dụng. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountId, BigDecimal balance, BigDecimal amount) {
        super("Tài khoản %s không đủ số dư: có %s, cần debit %s".formatted(accountId, balance, amount));
    }
}
