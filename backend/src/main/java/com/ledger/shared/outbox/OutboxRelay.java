package com.ledger.shared.outbox;

import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventSerde;
import com.ledger.shared.projection.ProjectionDispatcher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đọc các dòng outbox PENDING, dispatch tới projector, đánh dấu SENT — tất cả trong
 * cùng một transaction nên projection là "effectively-once": cập nhật read model và
 * đánh dấu SENT commit cùng nhau (ADR-0006). FOR UPDATE SKIP LOCKED cho phép nhiều
 * lần drain chạy song song mà không xử lý trùng một dòng.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 200;

    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbc;
    private final ProjectionDispatcher dispatcher;
    private final EventSerde serde;

    public OutboxRelay(
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbc,
            ProjectionDispatcher dispatcher,
            EventSerde serde) {
        this.transactionTemplate = transactionTemplate;
        this.jdbc = jdbc;
        this.dispatcher = dispatcher;
        this.serde = serde;
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
                DomainEvent event = serde.deserialize(row.eventType(), row.payload());
                dispatcher.dispatch(event);
                jdbc.update("UPDATE outbox SET status = 'SENT', sent_at = now() WHERE id = ?", row.id());
            }
            return rows.size();
        });
        return processed == null ? 0 : processed;
    }

    private record Pending(long id, String eventType, String payload) {}
}
