package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.api.TransactionResponse;
import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.query.AccountQueryService;
import com.ledger.shared.idempotency.IdempotencyConflictException;
import com.ledger.shared.idempotency.IdempotencyService;
import java.math.BigDecimal;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Idempotency: cùng key chỉ tạo hiệu lực một lần và trả lại đúng response cũ. */
@SpringBootTest
@ActiveProfiles("test")
class IdempotencyIntegrationTest {

    @Autowired
    private IdempotencyService idempotency;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void same_key_executes_action_once_and_replays_response() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        String key = "deposit-key-1";
        String hash = IdempotencyService.hashOf("deposit", alice, "1000");
        Supplier<TransactionResponse> deposit =
                () -> new TransactionResponse(money.deposit(new DepositCommand(alice, new BigDecimal("1000"))));

        TransactionResponse first = idempotency.execute(key, hash, TransactionResponse.class, deposit);
        TransactionResponse second = idempotency.execute(key, hash, TransactionResponse.class, deposit);

        // Lần 2 trả lại đúng txId cũ, KHÔNG chạy lại deposit.
        assertThat(second.txId()).isEqualTo(first.txId());
        assertThat(accountQuery.findBalance(alice).orElseThrow().balance()).isEqualByComparingTo("1000");
    }

    @Test
    void same_key_with_different_payload_is_rejected() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        String key = "deposit-key-2";

        idempotency.execute(key, IdempotencyService.hashOf("deposit", alice, "1000"), TransactionResponse.class,
                () -> new TransactionResponse(money.deposit(new DepositCommand(alice, new BigDecimal("1000")))));

        assertThatThrownBy(() -> idempotency.execute(
                key, IdempotencyService.hashOf("deposit", alice, "2000"), TransactionResponse.class,
                () -> new TransactionResponse(money.deposit(new DepositCommand(alice, new BigDecimal("2000"))))))
                .isInstanceOf(IdempotencyConflictException.class);

        // Chỉ lần đầu có hiệu lực.
        assertThat(accountQuery.findBalance(alice).orElseThrow().balance()).isEqualByComparingTo("1000");
    }

    @Test
    void failed_action_releases_key_for_retry() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        String key = "withdraw-key-1";
        String hash = IdempotencyService.hashOf("withdraw", alice, "500");

        // Lần đầu: rút khi số dư 0 -> action ném lỗi, key được nhả.
        assertThatThrownBy(() -> idempotency.execute(key, hash, TransactionResponse.class, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        // Cùng key dùng lại được vì lần trước đã nhả.
        TransactionResponse ok = idempotency.execute(key, hash, TransactionResponse.class,
                () -> new TransactionResponse("done"));
        assertThat(ok.txId()).isEqualTo("done");
    }
}
