package com.ledger.account.command;

import com.ledger.shared.domain.Command;
import java.math.BigDecimal;

public record TransferCommand(String fromAccountId, String toAccountId, BigDecimal amount) implements Command {
}
