package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OpenAccountCommandHandler {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;

    public OpenAccountCommandHandler(
            EventStore eventStore, RetryingTransactionExecutor executor, OutboxRelay relay) {
        this.eventStore = eventStore;
        this.executor = executor;
        this.relay = relay;
    }

    /**
     * @return id của tài khoản vừa mở.
     */
    public String handle(OpenAccountCommand command) {
        String accountId = UUID.randomUUID().toString();

        executor.execute(() -> {
            AccountAggregate account = new AccountAggregate();
            account.open(accountId, command.owner(), command.type());
            eventStore.append(accountId, AGGREGATE_TYPE, account.version(), account.uncommittedEvents());
            return null;
        });

        // Sau khi event đã commit (kèm outbox), project ngay để có read-your-writes.
        relay.drainQuietly();
        return accountId;
    }
}
