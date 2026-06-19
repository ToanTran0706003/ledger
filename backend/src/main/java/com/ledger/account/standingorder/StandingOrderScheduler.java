package com.ledger.account.standingorder;

import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ chạy các lệnh chuyển tiền đến hạn. Tắt trong test để tất định. */
@Component
@ConditionalOnProperty(
        prefix = "ledger.standing-orders",
        name = "scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StandingOrderScheduler {

    private final StandingOrderService service;

    public StandingOrderScheduler(StandingOrderService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${ledger.standing-orders.poll-interval-ms:5000}")
    public void poll() {
        service.runDue(Instant.now());
    }
}
