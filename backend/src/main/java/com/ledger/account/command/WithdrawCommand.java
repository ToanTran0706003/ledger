package com.ledger.account.command;

import com.ledger.shared.domain.Command;
import java.math.BigDecimal;

public record WithdrawCommand(String accountId, BigDecimal amount) implements Command {
}
