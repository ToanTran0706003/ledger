package com.ledger.saga;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestComplianceClient implements ComplianceClient {

    private final RestClient restClient;

    public RestComplianceClient(RestClient.Builder restClientBuilder,
            @Value("${ledger.saga.compliance-url}") String complianceUrl) {
        this.restClient = restClientBuilder.baseUrl(complianceUrl).build();
    }

    @Override
    public ComplianceDecision evaluate(String accountId, BigDecimal amount) {
        ComplianceDecision decision = restClient
                .post()
                .uri("/compliance/evaluate")
                .body(new ComplianceRequest(accountId, amount))
                .retrieve()
                .body(ComplianceDecision.class);
        if (decision == null) {
            throw new IllegalStateException("compliance-service returned empty decision");
        }
        return decision;
    }

    private record ComplianceRequest(String accountId, BigDecimal amount) {
    }
}
