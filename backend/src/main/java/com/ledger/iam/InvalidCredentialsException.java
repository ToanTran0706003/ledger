package com.ledger.iam;

/** Sai thông tin đăng nhập hoặc refresh token không hợp lệ. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Thông tin đăng nhập không hợp lệ");
    }
}
