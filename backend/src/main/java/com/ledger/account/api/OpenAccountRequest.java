package com.ledger.account.api;

import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.SystemAccounts;
import jakarta.validation.constraints.NotNull;

/** Chủ tài khoản lấy từ người dùng đang đăng nhập, không nhận từ body. currency để trống = VND. */
public record OpenAccountRequest(@NotNull AccountType type, String currency) {

    public String currencyOrDefault() {
        return currency != null && !currency.isBlank() ? currency : SystemAccounts.DEFAULT_CURRENCY;
    }
}
