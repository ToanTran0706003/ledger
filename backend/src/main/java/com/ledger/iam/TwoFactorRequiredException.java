package com.ledger.iam;

/** Đăng nhập đúng mật khẩu nhưng tài khoản đã bật 2FA và thiếu mã TOTP -> client cần nhập mã. */
public class TwoFactorRequiredException extends RuntimeException {

    public TwoFactorRequiredException() {
        super("Cần mã xác thực hai lớp (2FA).");
    }
}
