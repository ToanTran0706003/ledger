package com.ledger.saga;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final LedgerCoreClient ledgerCoreClient;
    private final ComplianceClient complianceClient;

    public SagaOrchestrator(LedgerCoreClient ledgerCoreClient, ComplianceClient complianceClient) {
        this.ledgerCoreClient = ledgerCoreClient;
        this.complianceClient = complianceClient;
    }

    public SagaResult withdraw(String accountId, BigDecimal amount, String authorization) {
        HoldResult hold;
        try {
            log.info("Saga step reserve: placing hold accountId={} amount={}", accountId, amount);
            hold = ledgerCoreClient.placeHold(accountId, amount, authorization);
            log.info("Saga step reserve: hold placed accountId={} holdId={}", accountId, hold.holdId());
        } catch (Exception ex) {
            log.warn("Saga failed before compensation: place hold failed accountId={}", accountId, ex);
            return SagaResult.failed(null, "Place hold failed: " + messageOf(ex));
        }

        String holdId = hold.holdId();
        try {
            log.info("Saga step decide: evaluating compliance accountId={} holdId={}", accountId, holdId);
            ComplianceDecision decision = complianceClient.evaluate(accountId, amount);

            if (decision.approved()) {
                log.info("Saga step commit: capturing hold accountId={} holdId={}", accountId, holdId);
                CaptureResult capture = ledgerCoreClient.captureHold(accountId, holdId, authorization);
                log.info("Saga completed accountId={} holdId={} txId={}", accountId, holdId, capture.txId());
                return SagaResult.completed(holdId, capture.txId());
            }

            log.info("Saga compensation: compliance rejected, releasing hold accountId={} holdId={}", accountId, holdId);
            ledgerCoreClient.releaseHold(accountId, holdId, authorization);
            log.info("Saga rejected accountId={} holdId={} reason={}", accountId, holdId, decision.reason());
            return SagaResult.rejected(holdId, decision.reason());
        } catch (Exception ex) {
            log.warn("Saga failed after hold; compensating accountId={} holdId={}", accountId, holdId, ex);
            compensate(accountId, holdId, authorization);
            return SagaResult.failed(holdId, messageOf(ex));
        }
    }

    private void compensate(String accountId, String holdId, String authorization) {
        try {
            log.info("Saga compensation: releasing hold accountId={} holdId={}", accountId, holdId);
            ledgerCoreClient.releaseHold(accountId, holdId, authorization);
            log.info("Saga compensation succeeded accountId={} holdId={}", accountId, holdId);
        } catch (Exception compensationFailure) {
            log.warn("Saga compensation failed accountId={} holdId={}", accountId, holdId, compensationFailure);
        }
    }

    private static String messageOf(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
