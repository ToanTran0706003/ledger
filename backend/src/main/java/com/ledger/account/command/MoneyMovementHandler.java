package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.account.fraud.FraudService;
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
    private final FraudService fraud;
    private final DailyLimitGuard dailyLimit;

    public MoneyMovementHandler(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            LedgerMetrics metrics,
            FraudService fraud,
            DailyLimitGuard dailyLimit) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.metrics = metrics;
        this.fraud = fraud;
        this.dailyLimit = dailyLimit;
    }

    public String deposit(DepositCommand command) {
        return run("deposit", MovementType.DEPOSIT,
                () -> vaultMove(command.accountId(), true, command.amount(), MovementType.DEPOSIT));
    }

    public String withdraw(WithdrawCommand command) {
        String txId = run("withdraw", MovementType.WITHDRAWAL,
                () -> vaultMove(command.accountId(), false, command.amount(), MovementType.WITHDRAWAL));
        fraud.evaluate(command.accountId(), command.amount()); // giám sát ghi nợ; tự đóng băng nếu nghi gian lận
        return txId;
    }

    public String transfer(TransferCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new IllegalArgumentException("Không thể chuyển tiền cho chính tài khoản đó");
        }
        String txId = run("transfer", MovementType.TRANSFER,
                () -> move(command.fromAccountId(), command.toAccountId(), command.amount(), MovementType.TRANSFER));
        fraud.evaluate(command.fromAccountId(), command.amount());
        return txId;
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

    // Nạp/rút: vế đối ứng là vault CỦA ĐÚNG TIỀN TỆ tài khoản (deposit: vault->account; withdraw:
    // account->vault). Hạn mức ngày chỉ áp cho vế ghi nợ của khách (withdraw).
    private String vaultMove(String accountId, boolean deposit, BigDecimal amount, MovementType movementType) {
        String txId = UUID.randomUUID().toString();

        AccountAggregate account = repository.load(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        String vaultId = SystemAccounts.vaultFor(account.currency());
        AccountAggregate vault = repository.load(vaultId).orElseThrow(() -> new AccountNotFoundException(vaultId));

        if (deposit) {
            vault.debit(txId, amount, movementType, accountId);
            account.credit(txId, amount, movementType, vaultId);
        } else {
            dailyLimit.check(accountId, amount);
            account.debit(txId, amount, movementType, vaultId);
            vault.credit(txId, amount, movementType, accountId);
        }

        repository.append(account);
        repository.append(vault);
        return txId;
    }

    // Chuyển tiền giữa hai tài khoản khách CÙNG TIỀN TỆ (khác tiền tệ phải qua FX). Load lại
    // aggregate mỗi lần thử để có version mới nhất; invariant không-âm kiểm tra trên vế debit.
    private String move(String fromId, String toId, BigDecimal amount, MovementType movementType) {
        String txId = UUID.randomUUID().toString();

        AccountAggregate from = repository.load(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
        AccountAggregate to = repository.load(toId).orElseThrow(() -> new AccountNotFoundException(toId));

        if (!from.currency().equals(to.currency())) {
            throw new IllegalArgumentException(
                    "Không thể chuyển giữa hai tiền tệ khác nhau (%s -> %s); hãy dùng quy đổi (FX)"
                            .formatted(from.currency(), to.currency()));
        }
        dailyLimit.check(fromId, amount);

        from.debit(txId, amount, movementType, toId);
        to.credit(txId, amount, movementType, fromId);

        repository.append(from);
        repository.append(to);

        return txId;
    }
}
