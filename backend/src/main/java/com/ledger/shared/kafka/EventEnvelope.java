package com.ledger.shared.kafka;

/** Gói event gửi qua Kafka: loại event + payload JSON (giữ nguyên dạng đã lưu ở event store). */
public record EventEnvelope(String eventType, String payload) {}
