package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAccountCommandHandler {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final Set<String> supportedCurrencies;

    public OpenAccountCommandHandler(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            @Value("${ledger.vault.currencies:VND}") java.util.List<String> currencies) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.supportedCurrencies = currencies.stream().map(String::trim).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return id của tài khoản vừa mở.
     */
    public String handle(OpenAccountCommand command) {
        // Chỉ cho mở tài khoản ở tiền tệ ĐÃ có vault (tránh tài khoản "mồ côi" không nạp/rút được).
        if (!supportedCurrencies.contains(command.currency())) {
            throw new IllegalArgumentException("Tiền tệ chưa được hỗ trợ: " + command.currency());
        }
        String accountId = UUID.randomUUID().toString();

        executor.execute(() -> {
            AccountAggregate account = new AccountAggregate();
            account.open(accountId, command.owner(), command.type(), command.currency());
            repository.append(account);
            return null;
        });

        // Sau khi event đã commit (kèm outbox), project ngay để có read-your-writes.
        relay.drainQuietly();
        return accountId;
    }
}
