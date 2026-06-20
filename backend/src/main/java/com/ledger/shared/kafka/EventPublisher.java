package com.ledger.shared.kafka;

/**
 * Phát một event đã commit (từ outbox relay) tới đường projection. Hai cài đặt chọn bằng cấu hình
 * {@code ledger.kafka.enabled}: in-process (mặc định) hoặc qua Kafka (event backbone, ADR-0023).
 */
public interface EventPublisher {

    void publish(String eventType, String payload);
}
