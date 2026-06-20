package com.ledger.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.audit.events.AccountOpenedEvent;
import com.ledger.audit.events.EventEnvelope;
import com.ledger.audit.events.MoneyPostedEvent;
import com.ledger.audit.repository.AuditBalanceRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final AuditBalanceRepository balanceRepository;

    public AuditEventConsumer(ObjectMapper objectMapper, AuditBalanceRepository balanceRepository) {
        this.objectMapper = objectMapper;
        this.balanceRepository = balanceRepository;
    }

    @KafkaListener(topics = "${ledger.audit.topic}")
    public void consume(String value) {
        try {
            EventEnvelope envelope = objectMapper.readValue(value, EventEnvelope.class);
            if ("AccountOpened".equals(envelope.eventType())) {
                handleAccountOpened(envelope.payload());
            } else if ("MoneyPosted".equals(envelope.eventType())) {
                handleMoneyPosted(envelope.payload());
            }
        } catch (JsonProcessingException ex) {
            log.warn("Ignoring malformed audit event", ex);
        }
    }

    private void handleAccountOpened(String payload) throws JsonProcessingException {
        AccountOpenedEvent event = objectMapper.readValue(payload, AccountOpenedEvent.class);
        balanceRepository.accountOpened(event.accountId(), event.currency());
    }

    private void handleMoneyPosted(String payload) throws JsonProcessingException {
        MoneyPostedEvent event = objectMapper.readValue(payload, MoneyPostedEvent.class);
        BigDecimal signedAmount = switch (event.direction()) {
            case CREDIT -> event.amount();
            case DEBIT -> event.amount().negate();
        };
        balanceRepository.moneyPosted(event.accountId(), event.txId(), signedAmount);
    }
}
