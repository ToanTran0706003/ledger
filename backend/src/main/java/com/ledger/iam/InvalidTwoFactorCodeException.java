package com.ledger.iam;

/** Mã TOTP không đúng (hoặc 2FA chưa thiết lập). */
public class InvalidTwoFactorCodeException extends RuntimeException {

    public InvalidTwoFactorCodeException() {
        super("Mã xác thực hai lớp không đúng.");
    }
}
