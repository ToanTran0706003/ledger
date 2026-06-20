package com.ledger.shared.kafka;

import com.ledger.shared.eventstore.EventSerde;
import com.ledger.shared.projection.ProjectionDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mặc định (Kafka tắt): projection chạy NGAY trong cùng transaction outbox — "effectively-once"
 * như trước (ADR-0006). Không cần hạ tầng ngoài.
 */
@Component
@ConditionalOnProperty(name = "ledger.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class InProcessEventPublisher implements EventPublisher {

    private final ProjectionDispatcher dispatcher;
    private final EventSerde serde;

    public InProcessEventPublisher(ProjectionDispatcher dispatcher, EventSerde serde) {
        this.dispatcher = dispatcher;
        this.serde = serde;
    }

    @Override
    public void publish(String eventType, String payload) {
        dispatcher.dispatch(serde.deserialize(eventType, payload));
    }
}
