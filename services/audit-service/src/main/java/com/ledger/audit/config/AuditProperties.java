package com.ledger.audit.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.audit")
public record AuditProperties(
        BigDecimal seedAmount,
        String topic
) {
}
