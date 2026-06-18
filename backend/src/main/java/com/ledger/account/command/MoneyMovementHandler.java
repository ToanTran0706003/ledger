package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.projection.ProjectionDispatcher;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Di chuyển tiền theo ghi sổ kép: mỗi thao tác sinh hai posting (debit nguồn,
 * credit đích) cùng txId, ghi atomic trong một transaction. Nạp = vault -> khách,
 * rút = khách -> vault, chuyển = khách -> khách (ADR-0005).
 */
@Service
public class MoneyMovementHandler {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final AccountRepository repository;
    private final ProjectionDispatcher dispatcher;

    public MoneyMovementHandler(EventStore eventStore, AccountRepository repository, ProjectionDispatcher dispatcher) {
        this.eventStore = eventStore;
        this.repository = repository;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public String deposit(DepositCommand command) {
        return move(SystemAccounts.VAULT_ID, command.accountId(), command.amount(), MovementType.DEPOSIT);
    }

    @Transactional
    public String withdraw(WithdrawCommand command) {
        return move(command.accountId(), SystemAccounts.VAULT_ID, command.amount(), MovementType.WITHDRAWAL);
    }

    @Transactional
    public String transfer(TransferCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new IllegalArgumentException("Không thể chuyển tiền cho chính tài khoản đó");
        }
        return move(command.fromAccountId(), command.toAccountId(), command.amount(), MovementType.TRANSFER);
    }

    // private -> chạy trong transaction của method public gọi nó (tránh self-invocation
    // làm mất hiệu lực @Transactional).
    private String move(String fromId, String toId, BigDecimal amount, MovementType movementType) {
        String txId = UUID.randomUUID().toString();

        AccountAggregate from = repository.load(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
        AccountAggregate to = repository.load(toId).orElseThrow(() -> new AccountNotFoundException(toId));

        // Vế nợ kiểm tra invariant không-âm; vế có luôn hợp lệ. Hai vế cùng txId, cân nhau.
        from.debit(txId, amount, movementType, toId);
        to.credit(txId, amount, movementType, fromId);

        // Append cả hai stream trong cùng transaction -> nguyên tử. Nếu một bên conflict
        // version, cả transaction rollback (ConcurrencyConflictException, retry ở Phase 3).
        eventStore.append(fromId, AGGREGATE_TYPE, from.version(), from.uncommittedEvents());
        eventStore.append(toId, AGGREGATE_TYPE, to.version(), to.uncommittedEvents());

        for (DomainEvent event : from.uncommittedEvents()) {
            dispatcher.dispatch(event);
        }
        for (DomainEvent event : to.uncommittedEvents()) {
            dispatcher.dispatch(event);
        }
        from.markEventsCommitted();
        to.markEventsCommitted();

        return txId;
    }
}
