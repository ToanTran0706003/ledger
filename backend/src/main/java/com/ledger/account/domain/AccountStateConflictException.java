package com.ledger.account.domain;

/**
 * Thao tác không hợp lệ với trạng thái hiện tại của tài khoản (vd đóng băng một tài khoản đã
 * đóng băng, mở băng tài khoản đang hoạt động). Là IllegalStateException để code gọi cũ vẫn bắt
 * được, nhưng tách riêng để map sang 409 mà không nuốt các IllegalStateException do lỗi hạ tầng.
 */
public class AccountStateConflictException extends IllegalStateException {

    public AccountStateConflictException(String message) {
        super(message);
    }
}
