package com.ledger.shared.kafka;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phát lại toàn bộ event log lên đường phát (Kafka khi bật) để rehydrate downstream service mới.
 * Thuộc {@code /admin/**} nên chỉ ADMIN gọi được (SecurityConfig). Xem {@link EventReplayService}.
 */
@RestController
@RequestMapping("/admin/events")
public class EventReplayController {

    private final EventReplayService replay;

    public EventReplayController(EventReplayService replay) {
        this.replay = replay;
    }

    @PostMapping("/republish")
    public Map<String, Object> republish() {
        return Map.of("republished", replay.republishAll());
    }
}
