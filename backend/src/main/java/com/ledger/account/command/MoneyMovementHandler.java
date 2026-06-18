package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.observability.LedgerMetrics;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Di chuyển tiền theo ghi sổ kép: mỗi thao tác sinh hai posting (debit nguồn,
 * credit đích) cùng txId, ghi atomic trong một transaction (ADR-0005). Nếu hai
 * request đụng nhau ở một tài khoản, optimistic concurrency từ chối một bên và
 * RetryingTransactionExecutor thử lại với version mới (ADR-0006).
 */
@Service
public class MoneyMovementHandler {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final LedgerMetrics metrics;

    public MoneyMovementHandler(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            LedgerMetrics metrics) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.metrics = metrics;
    }

    public String deposit(DepositCommand command) {
        return run("deposit", MovementType.DEPOSIT,
                () -> move(SystemAccounts.VAULT_ID, command.accountId(), command.amount(), MovementType.DEPOSIT));
    }

    public String withdraw(WithdrawCommand command) {
        return run("withdraw", MovementType.WITHDRAWAL,
                () -> move(command.accountId(), SystemAccounts.VAULT_ID, command.amount(), MovementType.WITHDRAWAL));
    }

    public String transfer(TransferCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new IllegalArgumentException("Không thể chuyển tiền cho chính tài khoản đó");
        }
        return run("transfer", MovementType.TRANSFER,
                () -> move(command.fromAccountId(), command.toAccountId(), command.amount(), MovementType.TRANSFER));
    }

    // Đo độ trễ (timer) + đếm throughput theo loại giao dịch (05-performance mục 8).
    private String run(String operation, MovementType type, Supplier<String> action) {
        return metrics.commandTimer(operation).record(() -> {
            String txId = executor.execute(action);
            relay.drainQuietly();
            metrics.recordTransaction(type.name());
            return txId;
        });
    }

    // Chạy trong transaction (do executor mở). Load lại aggregate mỗi lần thử để có
    // version mới nhất; invariant không-âm kiểm tra trên vế debit.
    private String move(String fromId, String toId, BigDecimal amount, MovementType movementType) {
        String txId = UUID.randomUUID().toString();

        AccountAggregate from = repository.load(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
        AccountAggregate to = repository.load(toId).orElseThrow(() -> new AccountNotFoundException(toId));

        from.debit(txId, amount, movementType, toId);
        to.credit(txId, amount, movementType, fromId);

        repository.append(from);
        repository.append(to);

        return txId;
    }
}
