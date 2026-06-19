package com.ledger.account.hold;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Đọc hold từ read model rm_hold — không replay, nhanh như CRUD. */
@Service
public class HoldQueryService {

    private final JdbcTemplate jdbc;

    public HoldQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HoldView> findByAccount(String accountId) {
        return jdbc.query(
                """
                SELECT hold_id, account_id, amount, status, reason, placed_at, expires_at, released_at
                FROM rm_hold
                WHERE account_id = ?
                ORDER BY placed_at DESC
                """,
                (rs, n) -> map(rs),
                accountId);
    }

    /** Các hold còn ACTIVE đã quá hạn — đầu vào cho scheduler tự nhả. */
    public List<DueHold> findDue(Instant now) {
        return jdbc.query(
                "SELECT hold_id, account_id FROM rm_hold WHERE status = 'ACTIVE' AND expires_at <= ?",
                (rs, n) -> new DueHold(rs.getString("account_id"), rs.getString("hold_id")),
                Timestamp.from(now));
    }

    public record DueHold(String accountId, String holdId) {}

    private static HoldView map(ResultSet rs) throws SQLException {
        Timestamp released = rs.getTimestamp("released_at");
        return new HoldView(
                rs.getString("hold_id"),
                rs.getString("account_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getString("reason"),
                rs.getTimestamp("placed_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                released == null ? null : released.toInstant());
    }
}
