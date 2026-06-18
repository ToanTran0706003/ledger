package com.ledger.account.query;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceAtView(String accountId, Instant asOf, BigDecimal balance) {
}
