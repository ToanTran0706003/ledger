package com.ledger.shared.snapshot;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lưu/đọc snapshot trạng thái aggregate (dạng JSON, không biết kiểu cụ thể —
 * tầng module tự (de)serialize). Snapshot là tối ưu: ghi đè theo aggregate_id.
 */
@Repository
public class SnapshotStore {

    private final JdbcTemplate jdbc;

    public SnapshotStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Snapshot> load(String aggregateId) {
        return jdbc
                .query(
                        "SELECT aggregate_version, state FROM snapshots WHERE aggregate_id = ?",
                        (rs, n) -> new Snapshot(rs.getInt("aggregate_version"), rs.getString("state")),
                        aggregateId)
                .stream()
                .findFirst();
    }

    public void save(String aggregateId, String aggregateType, int version, String stateJson) {
        jdbc.update(
                """
                INSERT INTO snapshots (aggregate_id, aggregate_type, aggregate_version, state)
                VALUES (?, ?, ?, ?::jsonb)
                ON CONFLICT (aggregate_id)
                DO UPDATE SET aggregate_version = EXCLUDED.aggregate_version,
                              state = EXCLUDED.state,
                              created_at = now()
                """,
                aggregateId, aggregateType, version, stateJson);
    }

    public record Snapshot(int version, String stateJson) {}
}
