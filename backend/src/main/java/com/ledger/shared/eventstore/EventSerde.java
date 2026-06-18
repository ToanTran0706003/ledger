package com.ledger.shared.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.shared.domain.DomainEvent;
import org.springframework.stereotype.Component;

/** (De)serialize event sang/từ JSON, dùng chung bởi event store và outbox relay. */
@Component
public class EventSerde {

    private final ObjectMapper mapper;
    private final EventTypeRegistry registry;

    public EventSerde(ObjectMapper mapper, EventTypeRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    public String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được event " + event.eventType(), e);
        }
    }

    public DomainEvent deserialize(String eventType, String payload) {
        try {
            return mapper.readValue(payload, registry.resolve(eventType));
        } catch (Exception e) {
            throw new IllegalStateException("Không deserialize được event " + eventType, e);
        }
    }
}
