package com.ledger.audit.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Kiểm tra tính toàn vẹn của hash-chain: với mỗi event, tái tính hash từ (prev_hash + nội dung)
 * và đối chiếu với hash đã lưu; đồng thời kiểm tra prev_hash nối đúng hash của event trước trong
 * cùng aggregate (event đầu tiên nối vào GENESIS = 64 số 0). Bất kỳ sửa đổi nào lên dòng event
 * đã ghi đều làm lệch hash và bị phát hiện. Biểu thức hash khớp y hệt lúc append và lúc backfill.
 */
@Service
public class HashChainVerifier {

    private final JdbcTemplate jdbc;

    public HashChainVerifier(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HashChainReport verify() {
        long total = jdbc.queryForObject("SELECT count(*) FROM events", Long.class);

        Long firstBroken = jdbc.query(
                """
                WITH chk AS (
                    SELECT global_seq, prev_hash, hash,
                           encode(sha256(convert_to(
                               prev_hash || aggregate_id || ':' || aggregate_version::text || ':'
                               || event_type || ':' || payload::text || ':' || COALESCE(metadata::text, ''),
                               'UTF8')), 'hex') AS recomputed,
                           lag(hash) OVER (PARTITION BY aggregate_id ORDER BY aggregate_version) AS expected_prev
                    FROM events
                )
                SELECT global_seq FROM chk
                WHERE hash <> recomputed
                   OR prev_hash IS DISTINCT FROM COALESCE(expected_prev, repeat('0', 64))
                ORDER BY global_seq
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getLong(1) : null);

        return new HashChainReport(firstBroken == null, total, firstBroken);
    }
}
