package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.projection.ProjectionDispatcher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAccountCommandHandler {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final ProjectionDispatcher dispatcher;

    public OpenAccountCommandHandler(EventStore eventStore, ProjectionDispatcher dispatcher) {
        this.eventStore = eventStore;
        this.dispatcher = dispatcher;
    }

    /**
     * @return id của tài khoản vừa mở.
     */
    @Transactional
    public String handle(OpenAccountCommand command) {
        String accountId = UUID.randomUUID().toString();

        AccountAggregate account = new AccountAggregate();
        account.open(accountId, command.owner(), command.type());

        eventStore.append(accountId, AGGREGATE_TYPE, account.version(), account.uncommittedEvents());

        // Phase 1/2: project đồng bộ trong cùng transaction. Transactional outbox
        // (tách write side khỏi projection) sẽ được đưa vào ở Phase 3.
        for (DomainEvent event : account.uncommittedEvents()) {
            dispatcher.dispatch(event);
        }
        account.markEventsCommitted();

        return accountId;
    }
}
