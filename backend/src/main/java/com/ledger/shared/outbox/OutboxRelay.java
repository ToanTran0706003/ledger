package com.ledger.shared.outbox;

import com.ledger.shared.kafka.EventPublisher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đọc các dòng outbox PENDING, phát event qua {@link EventPublisher}, đánh dấu SENT — tất cả trong
 * cùng một transaction (ADR-0006). Mặc định publisher chạy projection in-process ("effectively-once");
 * khi bật Kafka (ADR-0023), publisher gửi lên Kafka và projection chạy ở consumer (at-least-once,
 * idempotent). FOR UPDATE SKIP LOCKED cho phép nhiều lần drain song song không xử lý trùng dòng.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 200;

    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbc;
    private final EventPublisher publisher;

    public OutboxRelay(TransactionTemplate transactionTemplate, JdbcTemplate jdbc, EventPublisher publisher) {
        this.transactionTemplate = transactionTemplate;
        this.jdbc = jdbc;
        this.publisher = publisher;
    }

    /** Xử lý hết các dòng PENDING. */
    public void drain() {
        while (drainBatch() > 0) {
            // tiếp tục cho tới khi không còn dòng PENDING nào
        }
    }

    /** Drain nhưng nuốt lỗi — dùng sau commit và trong scheduler, không để hỏng luồng chính. */
    public void drainQuietly() {
        try {
            drain();
        } catch (RuntimeException e) {
            log.warn("Outbox drain lỗi, sẽ thử lại ở lần poll sau: {}", e.getMessage());
        }
    }

    private int drainBatch() {
        Integer processed = transactionTemplate.execute(status -> {
            List<Pending> rows = jdbc.query(
                    """
                    SELECT id, event_type, payload
                    FROM outbox
                    WHERE status = 'PENDING'
                    ORDER BY id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                    """,
                    (rs, n) -> new Pending(rs.getLong("id"), rs.getString("event_type"), rs.getString("payload")),
                    BATCH_SIZE);

            for (Pending row : rows) {
                publisher.publish(row.eventType(), row.payload());
                jdbc.update("UPDATE outbox SET status = 'SENT', sent_at = now() WHERE id = ?", row.id());
            }
            return rows.size();
        });
        return processed == null ? 0 : processed;
    }

    private record Pending(long id, String eventType, String payload) {}
}
