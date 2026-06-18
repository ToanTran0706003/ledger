package com.ledger.shared.projection;

import com.ledger.shared.domain.DomainEvent;
import java.util.List;
import org.springframework.stereotype.Component;

/** Phát một event tới mọi projector. Dùng bởi command handler và quá trình rebuild. */
@Component
public class ProjectionDispatcher {

    private final List<Projector> projectors;

    public ProjectionDispatcher(List<Projector> projectors) {
        this.projectors = projectors;
    }

    public void dispatch(DomainEvent event) {
        for (Projector projector : projectors) {
            projector.on(event);
        }
    }
}
