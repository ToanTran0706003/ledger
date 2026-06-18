package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OpenAccountCommandHandler {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;

    public OpenAccountCommandHandler(
            AccountRepository repository, RetryingTransactionExecutor executor, OutboxRelay relay) {
        this.repository = repository;
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
            repository.append(account);
            return null;
        });

        // Sau khi event đã commit (kèm outbox), project ngay để có read-your-writes.
        relay.drainQuietly();
        return accountId;
    }
}
