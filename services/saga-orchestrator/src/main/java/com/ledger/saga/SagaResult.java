package com.ledger.saga;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SagaResult(SagaStatus status, String holdId, String txId, String reason) {

    public static SagaResult completed(String holdId, String txId) {
        return new SagaResult(SagaStatus.COMPLETED, holdId, txId, null);
    }

    public static SagaResult rejected(String holdId, String reason) {
        return new SagaResult(SagaStatus.REJECTED, holdId, null, reason);
    }

    public static SagaResult failed(String holdId, String reason) {
        return new SagaResult(SagaStatus.FAILED, holdId, null, reason);
    }
}
