package com.ledger.account.domain;

/** Ném ra khi thao tác lên một accountId không tồn tại. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Không tìm thấy tài khoản: " + accountId);
    }
}
