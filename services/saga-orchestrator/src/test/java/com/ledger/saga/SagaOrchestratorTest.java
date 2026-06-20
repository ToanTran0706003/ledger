package com.ledger.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock
    private LedgerCoreClient ledgerCoreClient;

    @Mock
    private ComplianceClient complianceClient;

    @Test
    void approvedWithdrawalCapturesHoldWithoutRelease() {
        SagaOrchestrator orchestrator = new SagaOrchestrator(ledgerCoreClient, complianceClient);
        BigDecimal amount = new BigDecimal("123.45");
        when(ledgerCoreClient.placeHold("acct-1", amount, "Bearer token")).thenReturn(new HoldResult("hold-1"));
        when(complianceClient.evaluate("acct-1", amount)).thenReturn(new ComplianceDecision(true, "OK"));
        when(ledgerCoreClient.captureHold("acct-1", "hold-1", "Bearer token")).thenReturn(new CaptureResult("tx-1"));

        SagaResult result = orchestrator.withdraw("acct-1", amount, "Bearer token");

        assertThat(result.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(result.holdId()).isEqualTo("hold-1");
        assertThat(result.txId()).isEqualTo("tx-1");
        verify(ledgerCoreClient).captureHold("acct-1", "hold-1", "Bearer token");
        verify(ledgerCoreClient, never()).releaseHold("acct-1", "hold-1", "Bearer token");
    }

    @Test
    void rejectedWithdrawalReleasesHoldWithoutCapture() {
        SagaOrchestrator orchestrator = new SagaOrchestrator(ledgerCoreClient, complianceClient);
        BigDecimal amount = new BigDecimal("50000000.01");
        when(ledgerCoreClient.placeHold("acct-1", amount, "Bearer token")).thenReturn(new HoldResult("hold-1"));
        when(complianceClient.evaluate("acct-1", amount))
                .thenReturn(new ComplianceDecision(false, "Vuot nguong tuan thu"));

        SagaResult result = orchestrator.withdraw("acct-1", amount, "Bearer token");

        assertThat(result.status()).isEqualTo(SagaStatus.REJECTED);
        assertThat(result.holdId()).isEqualTo("hold-1");
        assertThat(result.reason()).isEqualTo("Vuot nguong tuan thu");
        verify(ledgerCoreClient).releaseHold("acct-1", "hold-1", "Bearer token");
        verify(ledgerCoreClient, never()).captureHold("acct-1", "hold-1", "Bearer token");
    }

    @Test
    void complianceFailureAfterHoldReleasesHoldAndReturnsFailed() {
        SagaOrchestrator orchestrator = new SagaOrchestrator(ledgerCoreClient, complianceClient);
        BigDecimal amount = new BigDecimal("123.45");
        when(ledgerCoreClient.placeHold("acct-1", amount, "Bearer token")).thenReturn(new HoldResult("hold-1"));
        when(complianceClient.evaluate("acct-1", amount)).thenThrow(new RuntimeException("compliance down"));

        SagaResult result = orchestrator.withdraw("acct-1", amount, "Bearer token");

        assertThat(result.status()).isEqualTo(SagaStatus.FAILED);
        assertThat(result.holdId()).isEqualTo("hold-1");
        assertThat(result.reason()).isEqualTo("compliance down");
        verify(ledgerCoreClient).releaseHold("acct-1", "hold-1", "Bearer token");
        verify(ledgerCoreClient, never()).captureHold("acct-1", "hold-1", "Bearer token");
    }
}
