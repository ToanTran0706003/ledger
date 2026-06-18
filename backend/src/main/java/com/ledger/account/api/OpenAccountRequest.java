package com.ledger.account.api;

import com.ledger.account.domain.AccountType;
import jakarta.validation.constraints.NotNull;

/** Chủ tài khoản lấy từ người dùng đang đăng nhập, không nhận từ body. */
public record OpenAccountRequest(@NotNull AccountType type) {
}
