package com.ledger.account.command;

import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.domain.Command;

public record OpenAccountCommand(String owner, AccountType type, String currency) implements Command {

    /** Mặc định mở tài khoản VND. */
    public OpenAccountCommand(String owner, AccountType type) {
        this(owner, type, SystemAccounts.DEFAULT_CURRENCY);
    }
}
