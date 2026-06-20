package com.ledger.audit.service;

import com.ledger.audit.config.AuditProperties;
import com.ledger.audit.repository.AuditBalanceRepository;
import com.ledger.audit.web.IntegrityResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class IntegrityService {

    private final AuditBalanceRepository balanceRepository;
    private final AuditProperties auditProperties;

    public IntegrityService(AuditBalanceRepository balanceRepository, AuditProperties auditProperties) {
        this.balanceRepository = balanceRepository;
        this.auditProperties = auditProperties;
    }

    public IntegrityResponse integrity() {
        var totals = balanceRepository.perCurrencyTotals();
        boolean balanced = totals.stream()
                .allMatch(total -> total.total().compareTo(auditProperties.seedAmount()) == 0);
        return new IntegrityResponse(totals, balanced);
    }

    public long accountCount() {
        return balanceRepository.countAccounts();
    }
}
