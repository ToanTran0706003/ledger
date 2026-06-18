package com.ledger.account.command;

import com.ledger.account.domain.AccountType;
import com.ledger.shared.domain.Command;

public record OpenAccountCommand(String owner, AccountType type) implements Command {
}
