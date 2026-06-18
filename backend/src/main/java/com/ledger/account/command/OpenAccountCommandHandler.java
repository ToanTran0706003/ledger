package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.projection.AccountBalanceProjector;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAccountCommandHandler {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final AccountBalanceProjector projector;

    public OpenAccountCommandHandler(EventStore eventStore, AccountBalanceProjector projector) {
        this.eventStore = eventStore;
        this.projector = projector;
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

        // Phase 1: project đồng bộ trong cùng transaction. Transactional outbox (tách
        // write side khỏi projection) sẽ được đưa vào ở Phase 3.
        for (DomainEvent event : account.uncommittedEvents()) {
            projector.on(event);
        }
        account.markEventsCommitted();

        return accountId;
    }
}
