package com.ledger.iam;

/** Username đã tồn tại khi đăng ký. */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username đã được dùng: " + username);
    }
}
