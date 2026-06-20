package com.ledger.saga;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestLedgerCoreClient implements LedgerCoreClient {

    private static final int HOLD_TTL_SECONDS = 300;

    private final RestClient restClient;

    public RestLedgerCoreClient(RestClient.Builder restClientBuilder,
            @Value("${ledger.saga.ledger-core-url}") String ledgerCoreUrl) {
        this.restClient = restClientBuilder.baseUrl(ledgerCoreUrl).build();
    }

    @Override
    public HoldResult placeHold(String accountId, BigDecimal amount, String authorization) {
        HoldResult result = restClient
                .post()
                .uri("/accounts/{accountId}/holds", accountId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(new PlaceHoldRequest(amount, HOLD_TTL_SECONDS))
                .retrieve()
                .body(HoldResult.class);
        if (result == null || result.holdId() == null || result.holdId().isBlank()) {
            throw new IllegalStateException("ledger-core returned empty holdId");
        }
        return result;
    }

    @Override
    public CaptureResult captureHold(String accountId, String holdId, String authorization) {
        CaptureResult result = restClient
                .post()
                .uri("/accounts/{accountId}/holds/{holdId}/capture", accountId, holdId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .retrieve()
                .body(CaptureResult.class);
        if (result == null || result.txId() == null || result.txId().isBlank()) {
            throw new IllegalStateException("ledger-core returned empty txId");
        }
        return result;
    }

    @Override
    public void releaseHold(String accountId, String holdId, String authorization) {
        restClient
                .post()
                .uri("/accounts/{accountId}/holds/{holdId}/release", accountId, holdId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .toBodilessEntity();
    }

    private record PlaceHoldRequest(BigDecimal amount, int ttlSeconds) {
    }
}
