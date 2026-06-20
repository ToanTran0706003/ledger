package com.ledger.shared.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka bật: outbox relay PUBLISH event lên topic Kafka (event backbone, ADR-0023); một consumer
 * sẽ projection bất đồng bộ. Gửi đồng bộ ({@code .get()}) TRONG transaction outbox rồi mới đánh dấu
 * SENT -> nếu gửi lỗi thì rollback, dòng vẫn PENDING và được thử lại (giao nhận at-least-once;
 * consumer idempotent vì projection là UPSERT).
 */
@Component
@ConditionalOnProperty(name = "ledger.kafka.enabled", havingValue = "true")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafka,
            ObjectMapper mapper,
            @Value("${ledger.kafka.topic:ledger.events}") String topic) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
    }

    @Override
    public void publish(String eventType, String payload) {
        try {
            String envelope = mapper.writeValueAsString(new EventEnvelope(eventType, payload));
            kafka.send(topic, envelope).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi gửi event lên Kafka: " + eventType, e);
        } catch (Exception e) {
            throw new IllegalStateException("Không gửi được event lên Kafka: " + eventType, e);
        }
    }
}
