package com.ledger.compliance;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

    private static final String APPROVED_REASON = "OK";
    private static final String REJECTED_REASON = "Vượt ngưỡng tuân thủ";

    private final BigDecimal threshold;

    public ComplianceService(@Value("${ledger.compliance.threshold:50000000}") BigDecimal threshold) {
        this.threshold = threshold;
    }

    public ComplianceDecision evaluate(ComplianceRequest request) {
        boolean approved = request.amount().compareTo(threshold) <= 0;
        return new ComplianceDecision(approved, approved ? APPROVED_REASON : REJECTED_REASON);
    }
}
