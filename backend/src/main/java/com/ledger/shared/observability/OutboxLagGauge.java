package com.ledger.shared.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Projection lag — chỉ số sống còn trong CQRS: số event đã commit nhưng read model
 * chưa cập nhật (outbox còn PENDING). Prometheus scrape gauge này định kỳ.
 */
@Component
public class OutboxLagGauge {

    private final JdbcTemplate jdbc;

    public OutboxLagGauge(JdbcTemplate jdbc, MeterRegistry registry) {
        this.jdbc = jdbc;
        Gauge.builder("ledger.outbox.pending", this, OutboxLagGauge::pendingCount)
                .description("Số event chưa được project (projection lag)")
                .register(registry);
    }

    private double pendingCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status = 'PENDING'", Integer.class);
        return count == null ? 0 : count;
    }
}
