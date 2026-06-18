package com.ledger.shared.projection;

import com.ledger.shared.domain.DomainEvent;

/** Cập nhật một (hoặc nhiều) read model từ một domain event. Phải idempotent. */
public interface Projector {

    void on(DomainEvent event);
}
