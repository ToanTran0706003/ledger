package com.ledger.account.domain;

/** Ném ra khi cố ghi nợ một tài khoản đang bị đóng băng. */
public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException(String accountId) {
        super("Tài khoản " + accountId + " đang bị đóng băng, không thể ghi nợ");
    }
}
