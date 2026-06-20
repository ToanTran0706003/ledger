package com.ledger.shared.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.shared.eventstore.EventSerde;
import com.ledger.shared.projection.ProjectionDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer đường projection qua Kafka (ADR-0023): nhận event từ topic, projection vào read model.
 * Idempotent (projector dùng UPSERT) nên giao nhận at-least-once an toàn. Chỉ active khi
 * {@code ledger.kafka.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "ledger.kafka.enabled", havingValue = "true")
public class KafkaProjectionConsumer {

    private final ProjectionDispatcher dispatcher;
    private final EventSerde serde;
    private final ObjectMapper mapper;

    public KafkaProjectionConsumer(ProjectionDispatcher dispatcher, EventSerde serde, ObjectMapper mapper) {
        this.dispatcher = dispatcher;
        this.serde = serde;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${ledger.kafka.topic:ledger.events}", groupId = "${ledger.kafka.group-id:ledger-projector}")
    @Transactional
    public void onMessage(String message) {
        EventEnvelope envelope;
        try {
            envelope = mapper.readValue(message, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalStateException("Không đọc được envelope event từ Kafka", e);
        }
        dispatcher.dispatch(serde.deserialize(envelope.eventType(), envelope.payload()));
    }
}
