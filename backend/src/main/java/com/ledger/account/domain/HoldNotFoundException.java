package com.ledger.account.domain;

/** Không tìm thấy hold đang hoạt động với holdId đã cho trên tài khoản. */
public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(String accountId, String holdId) {
        super("Không tìm thấy hold đang hoạt động '" + holdId + "' trên tài khoản " + accountId);
    }
}
