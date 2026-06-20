package com.ledger.shared.kafka;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Phát lại TOÀN BỘ event log (từ event store) qua {@link EventPublisher} — dùng để "rehydrate" một
 * consumer/service mới tham gia (vd audit-service) với đầy đủ lịch sử từ GENESIS. Đây là lợi ích cốt
 * lõi của event sourcing: log bất biến có thể tua lại để dựng bất kỳ view nào. Khi bật Kafka, đây là
 * cách bơm lịch sử lên topic cho downstream; consumer idempotent nên phát lại an toàn (ADR-0024).
 */
@Service
public class EventReplayService {

    private final JdbcTemplate jdbc;
    private final EventPublisher publisher;

    public EventReplayService(JdbcTemplate jdbc, EventPublisher publisher) {
        this.jdbc = jdbc;
        this.publisher = publisher;
    }

    /** Phát lại mọi event theo thứ tự global_seq. Trả về số event đã phát. */
    public long republishAll() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT event_type, payload::text AS payload FROM events ORDER BY global_seq");
        for (Map<String, Object> row : rows) {
            publisher.publish((String) row.get("event_type"), (String) row.get("payload"));
        }
        return rows.size();
    }
}
