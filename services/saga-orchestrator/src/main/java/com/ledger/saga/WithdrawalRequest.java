package com.ledger.saga;

import java.math.BigDecimal;

public record WithdrawalRequest(String accountId, BigDecimal amount) {
}
