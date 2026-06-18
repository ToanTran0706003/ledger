package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.Direction;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.account.domain.MovementType;
import com.ledger.account.query.AccountQueryService;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Outbox đảm bảo không mất projection: một event đã commit nhưng CHƯA được drain
 * (mô phỏng crash giữa commit và projection) vẫn được relay project sau đó.
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxRelayIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private EventStore eventStore;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        vaultSeed.seedIfAbsent();
    }

    private int pendingOutbox() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status = 'PENDING'", Integer.class);
    }

    @Test
    void committed_event_not_yet_drained_is_projected_by_relay() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        // open đã drain -> số dư 0, không còn PENDING.
        assertThat(accountQuery.findBalance(alice).orElseThrow().balance()).isEqualByComparingTo("0");
        assertThat(pendingOutbox()).isZero();

        // Append một posting trực tiếp, KHÔNG drain (mô phỏng app crash trước khi project).
        DomainEvent credit = new MoneyPosted(
                alice, "tx-orphan", Direction.CREDIT, new BigDecimal("500"),
                MovementType.DEPOSIT, "SYSTEM_VAULT", Instant.now(), null);
        eventStore.append(alice, "Account", 1, List.of(credit));

        // Read model chưa phản ánh, nhưng outbox đã giữ event (không mất).
        assertThat(accountQuery.findBalance(alice).orElseThrow().balance()).isEqualByComparingTo("0");
        assertThat(pendingOutbox()).isEqualTo(1);

        // Relay chạy -> event được project, outbox đánh dấu SENT.
        relay.drain();

        assertThat(accountQuery.findBalance(alice).orElseThrow().balance()).isEqualByComparingTo("500");
        assertThat(pendingOutbox()).isZero();
    }
}
