package com.ledger.shared.eventstore;

import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;

/**
 * Kho event append-only. append ghi nối tiếp; loadStream* đọc lại để replay;
 * loadAll phục vụ rebuild read model.
 */
public interface EventStore {

    /**
     * Ghi các event mới cho một aggregate.
     *
     * @param expectedVersion version aggregate lúc load. Mỗi event mới nhận version
     *                        tăng dần từ đây. Nếu có request khác đã ghi version đó,
     *                        DB từ chối và ta ném {@link ConcurrencyConflictException}.
     */
    void append(String aggregateId, String aggregateType, int expectedVersion, List<DomainEvent> events);

    /** Đọc toàn bộ event của một aggregate theo thứ tự version (để replay). */
    List<DomainEvent> loadStream(String aggregateId);

    /** Đọc event của một aggregate có version > afterVersion (replay sau snapshot). */
    List<DomainEvent> loadStreamAfter(String aggregateId, int afterVersion);

    /** Đọc event của một aggregate xảy ra tại hoặc trước thời điểm asOf (time-travel). */
    List<DomainEvent> loadStreamUntil(String aggregateId, Instant asOf);

    /** Đọc mọi event của toàn hệ thống theo thứ tự global_seq (để rebuild read model). */
    List<DomainEvent> loadAll();
}
