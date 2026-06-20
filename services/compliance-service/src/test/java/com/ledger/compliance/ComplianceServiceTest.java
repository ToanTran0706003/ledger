package com.ledger.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ComplianceServiceTest {

    @Test
    void approvesAmountsAtOrBelowThreshold() {
        ComplianceService service = new ComplianceService(new BigDecimal("50000000"));

        ComplianceDecision decision = service.evaluate(new ComplianceRequest("acct-1", new BigDecimal("50000000")));

        assertThat(decision.approved()).isTrue();
        assertThat(decision.reason()).isEqualTo("OK");
    }

    @Test
    void rejectsAmountsAboveThreshold() {
        ComplianceService service = new ComplianceService(new BigDecimal("50000000"));

        ComplianceDecision decision = service.evaluate(new ComplianceRequest("acct-1", new BigDecimal("50000000.01")));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("Vượt ngưỡng tuân thủ");
    }
}
