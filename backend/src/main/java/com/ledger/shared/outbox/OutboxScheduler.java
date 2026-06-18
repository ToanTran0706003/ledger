package com.ledger.shared.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Poll outbox định kỳ — lưới an toàn để projection vẫn chạy nếu drain-sau-commit lỡ
 * thất bại hoặc app từng crash giữa chừng (event đã commit vẫn PENDING -> được gửi lại).
 * Tắt trong test (ledger.outbox.scheduler-enabled=false) để test tất định.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.outbox", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxRelay relay;

    public OutboxScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${ledger.outbox.poll-interval-ms:1000}")
    public void poll() {
        relay.drainQuietly();
    }
}
