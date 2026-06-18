package com.ledger.account.command;

import com.ledger.shared.domain.Command;
import java.math.BigDecimal;

public record DepositCommand(String accountId, BigDecimal amount) implements Command {
}
