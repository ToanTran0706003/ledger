package com.ledger.account.command;

import com.ledger.shared.domain.Command;

public record ReverseTransactionCommand(String originalTxId) implements Command {
}
