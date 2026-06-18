package com.ledger.shared.eventstore;

import com.ledger.shared.domain.DomainEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Ánh xạ tên logic của event (event_type trong DB) sang class để deserialize.
 * Mỗi module tự đăng ký event của mình (giữ ranh giới module — xem ADR-0001),
 * nên kernel shared không cần biết về event cụ thể của account/ledger.
 */
public class EventTypeRegistry {

    private final Map<String, Class<? extends DomainEvent>> byName = new HashMap<>();

    public void register(Class<? extends DomainEvent> eventClass) {
        byName.put(eventClass.getSimpleName(), eventClass);
    }

    public Class<? extends DomainEvent> resolve(String eventType) {
        Class<? extends DomainEvent> clazz = byName.get(eventType);
        if (clazz == null) {
            throw new IllegalStateException("Chưa đăng ký event_type: " + eventType);
        }
        return clazz;
    }
}
