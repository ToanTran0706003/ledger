package com.ledger.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Metrics nghiệp vụ cho Prometheus (xem 05-performance mục 8): độ trễ lệnh, throughput
 * theo loại giao dịch, và tỉ lệ xung đột optimistic concurrency. Projection lag đo riêng
 * bằng gauge {@link OutboxLagGauge}.
 */
@Component
public class LedgerMetrics {

    private final MeterRegistry registry;

    public LedgerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer commandTimer(String operation) {
        return Timer.builder("ledger.command.duration")
                .description("Thời gian xử lý một lệnh ghi tiền")
                .tag("operation", operation)
                .register(registry);
    }

    public void recordTransaction(String type) {
        registry.counter("ledger.transactions", "type", type).increment();
    }

    public void recordConflict() {
        registry.counter("ledger.concurrency.conflicts").increment();
    }
}
