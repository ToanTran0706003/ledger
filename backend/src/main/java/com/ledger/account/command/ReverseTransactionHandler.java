package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.Direction;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.TransactionNotFoundException;
import com.ledger.shared.eventstore.EventSerde;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Đảo một giao dịch bằng bút toán bù (KHÔNG xóa lịch sử): tìm các posting của txId gốc
 * rồi tạo posting ngược lại (credit{@literal <->}debit) cùng một txId mới, movementType
 * REVERSAL, trỏ về txId gốc. Nếu vế debit khi đảo làm tài khoản âm (tiền đã tiêu) thì
 * invariant từ chối -> không đảo được.
 */
@Service
public class ReverseTransactionHandler {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final JdbcTemplate jdbc;
    private final EventSerde serde;

    public ReverseTransactionHandler(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            JdbcTemplate jdbc,
            EventSerde serde) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.jdbc = jdbc;
        this.serde = serde;
    }

    public String reverse(ReverseTransactionCommand command) {
        String newTxId = executor.execute(() -> {
            List<MoneyPosted> originals = findPostings(command.originalTxId());
            if (originals.isEmpty()) {
                throw new TransactionNotFoundException(command.originalTxId());
            }
            String txId = UUID.randomUUID().toString();
            for (MoneyPosted original : originals) {
                if (original.movementType() == MovementType.GENESIS) {
                    throw new IllegalArgumentException("Không thể đảo bút toán GENESIS");
                }
                AccountAggregate account = repository
                        .load(original.accountId())
                        .orElseThrow(() -> new AccountNotFoundException(original.accountId()));

                if (original.direction() == Direction.CREDIT) {
                    account.debit(txId, original.amount(), MovementType.REVERSAL,
                            original.counterpartyAccountId(), command.originalTxId());
                } else {
                    account.credit(txId, original.amount(), MovementType.REVERSAL,
                            original.counterpartyAccountId(), command.originalTxId());
                }
                repository.append(account);
            }
            return txId;
        });

        relay.drainQuietly();
        return newTxId;
    }

    private List<MoneyPosted> findPostings(String originalTxId) {
        return jdbc.query(
                """
                SELECT event_type, payload FROM events
                WHERE event_type = 'MoneyPosted' AND payload->>'txId' = ?
                ORDER BY global_seq
                """,
                (rs, n) -> (MoneyPosted) serde.deserialize(rs.getString("event_type"), rs.getString("payload")),
                originalTxId);
    }
}
