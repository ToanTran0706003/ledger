package com.ledger.account.hold;

import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ tự nhả các hold đã quá hạn. Tắt trong test để tất định. */
@Component
@ConditionalOnProperty(
        prefix = "ledger.holds",
        name = "scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class HoldExpiryScheduler {

    private final HoldService holdService;

    public HoldExpiryScheduler(HoldService holdService) {
        this.holdService = holdService;
    }

    @Scheduled(fixedDelayString = "${ledger.holds.poll-interval-ms:5000}")
    public void poll() {
        holdService.expireDue(Instant.now());
    }
}
