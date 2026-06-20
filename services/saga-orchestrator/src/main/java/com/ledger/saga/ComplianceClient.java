package com.ledger.saga;

import java.math.BigDecimal;

public interface ComplianceClient {

    ComplianceDecision evaluate(String accountId, BigDecimal amount);
}
