package com.ledger.compliance;

import java.math.BigDecimal;

public record ComplianceRequest(String accountId, BigDecimal amount) {
}
