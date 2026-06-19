package com.ledger.shared.eventstore;

import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.observability.CorrelationContext;
import java.sql.Timestamp;
import java.time.Instant;
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

    // Hash khởi điểm cho event đầu tiên của mỗi aggregate (không có event trước).
    private static final String GENESIS_HASH = "0".repeat(64);

    private final JdbcTemplate jdbc;
    private final EventSerde serde;
    private final CorrelationContext correlationContext;

    public JdbcEventStore(JdbcTemplate jdbc, EventSerde serde, CorrelationContext correlationContext) {
        this.jdbc = jdbc;
        this.serde = serde;
        this.correlationContext = correlationContext;
    }

    @Override
    public void append(String aggregateId, String aggregateType, int expectedVersion, List<DomainEvent> events) {
        int version = expectedVersion;
        String metadata = correlationContext.currentMetadataJson(); // null nếu không có request context
        String prevHash = latestHash(aggregateId);
        try {
            for (DomainEvent event : events) {
                version++;
                UUID eventId = UUID.randomUUID();
                String payload = serde.serialize(event);

                // hash = SHA-256(prevHash + nội dung + metadata). payload/metadata dùng dạng canonical
                // jsonb của DB ((?::jsonb)::text) để verify tái tính trùng khít, không phụ thuộc cách Java
                // serialize. metadata (userId/ip/correlationId) được bảo vệ luôn; null -> coi như chuỗi rỗng.
                // BIỂU THỨC HASH PHẢI KHỚP y hệt ở V12 (backfill) và HashChainVerifier.
                String hash = jdbc.queryForObject(
                        """
                        INSERT INTO events (event_id, aggregate_id, aggregate_type, aggregate_version, event_type, payload, metadata, prev_hash, hash)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?,
                                encode(sha256(convert_to(
                                    ? || ? || ':' || ?::text || ':' || ? || ':' || (?::jsonb)::text
                                    || ':' || COALESCE((?::jsonb)::text, ''), 'UTF8')), 'hex'))
                        RETURNING hash
                        """,
                        String.class,
                        eventId, aggregateId, aggregateType, version, event.eventType(), payload, metadata, prevHash,
                        prevHash, aggregateId, version, event.eventType(), payload, metadata);

                jdbc.update(
                        "INSERT INTO outbox (event_id, event_type, payload) VALUES (?, ?, ?::jsonb)",
                        eventId, event.eventType(), payload);

                prevHash = hash; // event kế tiếp trong cùng aggregate nối vào hash vừa tạo
            }
        } catch (DuplicateKeyException e) {
            // Vi phạm uq_aggregate_version: một request khác đã ghi version này trước.
            throw new ConcurrencyConflictException(aggregateId, version);
        }
    }

    // Hash của event mới nhất trong aggregate (đầu chuỗi để nối tiếp), hoặc GENESIS nếu chưa có.
    private String latestHash(String aggregateId) {
        List<String> hashes = jdbc.query(
                "SELECT hash FROM events WHERE aggregate_id = ? ORDER BY aggregate_version DESC LIMIT 1",
                (rs, n) -> rs.getString(1),
                aggregateId);
        return hashes.isEmpty() ? GENESIS_HASH : hashes.get(0);
    }

    @Override
    public List<DomainEvent> loadStream(String aggregateId) {
        return jdbc.query(
                "SELECT event_type, payload FROM events WHERE aggregate_id = ? ORDER BY aggregate_version",
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")),
                aggregateId);
    }

    @Override
    public List<DomainEvent> loadStreamAfter(String aggregateId, int afterVersion) {
        return jdbc.query(
                """
                SELECT event_type, payload FROM events
                WHERE aggregate_id = ? AND aggregate_version > ?
                ORDER BY aggregate_version
                """,
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")),
                aggregateId, afterVersion);
    }

    @Override
    public List<DomainEvent> loadStreamUntil(String aggregateId, Instant asOf) {
        return jdbc.query(
                """
                SELECT event_type, payload FROM events
                WHERE aggregate_id = ? AND occurred_at <= ?
                ORDER BY aggregate_version
                """,
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")),
                aggregateId, Timestamp.from(asOf));
    }

    @Override
    public List<DomainEvent> loadAll() {
        return jdbc.query(
                "SELECT event_type, payload FROM events ORDER BY global_seq",
                (rs, n) -> serde.deserialize(rs.getString("event_type"), rs.getString("payload")));
    }
}
