package com.ledger.shared.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.shared.domain.DomainEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Cài đặt event store trên PostgreSQL bằng JDBC thuần (ADR-0002).
 * Dùng JDBC thay JPA để kiểm soát chặt việc append-only — không có path UPDATE/DELETE.
 */
@Repository
public class JdbcEventStore implements EventStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final EventTypeRegistry registry;

    public JdbcEventStore(JdbcTemplate jdbc, ObjectMapper mapper, EventTypeRegistry registry) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public void append(String aggregateId, String aggregateType, int expectedVersion, List<DomainEvent> events) {
        int version = expectedVersion;
        try {
            for (DomainEvent event : events) {
                version++;
                jdbc.update(
                        """
                        INSERT INTO events (event_id, aggregate_id, aggregate_type, aggregate_version, event_type, payload)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb)
                        """,
                        UUID.randomUUID(), aggregateId, aggregateType, version, event.eventType(), serialize(event));
            }
        } catch (DuplicateKeyException e) {
            // Vi phạm uq_aggregate_version: một request khác đã ghi version này trước.
            throw new ConcurrencyConflictException(aggregateId, version);
        }
    }

    @Override
    public List<DomainEvent> loadStream(String aggregateId) {
        return jdbc.query(
                "SELECT event_type, payload FROM events WHERE aggregate_id = ? ORDER BY aggregate_version",
                (rs, n) -> deserialize(rs.getString("event_type"), rs.getString("payload")),
                aggregateId);
    }

    @Override
    public List<DomainEvent> loadAll() {
        return jdbc.query(
                "SELECT event_type, payload FROM events ORDER BY global_seq",
                (rs, n) -> deserialize(rs.getString("event_type"), rs.getString("payload")));
    }

    private String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được event " + event.eventType(), e);
        }
    }

    private DomainEvent deserialize(String eventType, String payload) {
        try {
            return mapper.readValue(payload, registry.resolve(eventType));
        } catch (Exception e) {
            throw new IllegalStateException("Không deserialize được event " + eventType, e);
        }
    }
}
