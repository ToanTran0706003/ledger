package com.ledger.audit.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ledger.audit.repository.AuditBalanceRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AuditEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AuditBalanceRepository balanceRepository = org.mockito.Mockito.mock(AuditBalanceRepository.class);
    private final AuditEventConsumer consumer = new AuditEventConsumer(objectMapper, balanceRepository);

    @Test
    void consumesAccountOpened() {
        consumer.consume("""
                {
                  "eventType": "AccountOpened",
                  "payload": "{\\"accountId\\":\\"acct-1\\",\\"owner\\":\\"Alice\\",\\"type\\":\\"ASSET\\",\\"currency\\":\\"VND\\",\\"openedAt\\":\\"2026-06-20T00:00:00Z\\"}"
                }
                """);

        verify(balanceRepository).accountOpened("acct-1", "VND");
    }

    @Test
    void consumesMoneyPostedAsSignedAmount() {
        consumer.consume("""
                {
                  "eventType": "MoneyPosted",
                  "payload": "{\\"accountId\\":\\"acct-1\\",\\"txId\\":\\"tx-1\\",\\"direction\\":\\"DEBIT\\",\\"amount\\":\\"12.34\\",\\"movementType\\":\\"TRANSFER\\",\\"counterpartyAccountId\\":null,\\"postedAt\\":\\"2026-06-20T00:00:00Z\\",\\"reversalOfTxId\\":null}"
                }
                """);

        verify(balanceRepository).moneyPosted("acct-1", "tx-1", new BigDecimal("-12.34"));
    }

    @Test
    void ignoresOtherEvents() {
        consumer.consume("""
                {"eventType":"SomethingElse","payload":"{}"}
                """);

        verifyNoInteractions(balanceRepository);
    }
}
