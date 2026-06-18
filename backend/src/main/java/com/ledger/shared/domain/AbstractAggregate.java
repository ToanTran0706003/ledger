package com.ledger.shared.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Nền tảng cho mọi aggregate event-sourced. Trạng thái không được lưu trực tiếp;
 * nó được dựng lại bằng cách replay chuỗi event ({@link #replay}). Khi xử lý command,
 * aggregate phát event mới qua {@link #raise} thay vì sửa trạng thái rồi mới ghi.
 */
public abstract class AbstractAggregate {

    private int version = 0; // số event đã được persist = version của event cuối cùng
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    /** Áp một event lên trạng thái. Cài đặt ở aggregate con, không có side-effect ngoài việc đổi field. */
    protected abstract void apply(DomainEvent event);

    /** Dựng lại trạng thái từ lịch sử (load từ event store). Không tạo event mới. */
    public void replay(List<DomainEvent> history) {
        for (DomainEvent event : history) {
            apply(event);
            version++;
        }
    }

    /**
     * Đặt version khi khôi phục từ snapshot. Aggregate con tự khôi phục các field trạng
     * thái rồi gọi hàm này; sau đó replay các event SAU version snapshot.
     */
    protected void restoreVersion(int version) {
        this.version = version;
    }

    /** Phát một event mới: áp ngay vào trạng thái và ghi nhận để persist sau. */
    protected void raise(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    public List<DomainEvent> uncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void markEventsCommitted() {
        version += uncommittedEvents.size();
        uncommittedEvents.clear();
    }

    /** Version đã persist — dùng làm expectedVersion khi append (optimistic concurrency). */
    public int version() {
        return version;
    }
}
