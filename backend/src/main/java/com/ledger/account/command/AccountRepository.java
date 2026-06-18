package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Nạp AccountAggregate bằng cách replay event của nó từ event store. */
@Service
public class AccountRepository {

    private final EventStore eventStore;

    public AccountRepository(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Optional<AccountAggregate> load(String accountId) {
        List<DomainEvent> history = eventStore.loadStream(accountId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        AccountAggregate aggregate = new AccountAggregate();
        aggregate.replay(history);
        return Optional.of(aggregate);
    }
}
