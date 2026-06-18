package com.ledger.shared.eventstore;

import com.ledger.shared.domain.DomainEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Cài đặt event store trên PostgreSQL bằng JDBC thuần (ADR-0002). Mỗi lần append,
 * ghi event vào bảng events VÀ một bản vào outbox trong cùng transaction
 * (transactional outbox, ADR-0006) — đảm bảo không mất event cho projection.
 */
@Repository
public class JdbcEventStore implements EventStore {

    private final JdbcTemplate jdbc;
    private final EventSerde serde;

    public JdbcEventStore(JdbcTemplate jdbc, EventSerde serde) {
        this.jdbc = jdbc;
        this.serde = serde;
    }

    @Override
    public void append(String aggregateId, String aggregateType, int expectedVersion, List<DomainEvent> events) {
        int version = expectedVersion;
        try {
            for (DomainEvent event : events) {
                version++;
                UUID eventId = UUID.randomUUID();
                String payload = serde.serialize(event);

                jdbc.update(
                        """
                        INSERT INTO events (event_id, aggregate_id, aggregate_type, aggregate_version, event_type, payload)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb)
                        """,
                        eventId, aggregateId, aggregateType, version, event.eventType(), payload);

                jdbc.update(
                        "INSERT INTO outbox (event_id, event_type, payload) VALUES (?, ?, ?::jsonb)",
                        eventId, event.eventType(), payload);
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
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")),
                aggregateId);
    }

    @Override
    public List<DomainEvent> loadAll() {
        return jdbc.query(
                "SELECT event_type, payload FROM events ORDER BY global_seq",
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")));
    }
}
