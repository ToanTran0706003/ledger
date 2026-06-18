package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Observability: metric nghiệp vụ tăng đúng (counter throughput + timer độ trễ). */
@SpringBootTest
@ActiveProfiles("test")
class MetricsIntegrationTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

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

    @Test
    void deposit_increments_counter_and_records_timer() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        double before = depositCount();

        money.deposit(new DepositCommand(alice, new BigDecimal("100")));

        assertThat(depositCount()).isEqualTo(before + 1);

        Timer timer = registry.find("ledger.command.duration").tag("operation", "deposit").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isGreaterThanOrEqualTo(1);
    }

    private double depositCount() {
        Counter counter = registry.find("ledger.transactions").tag("type", "DEPOSIT").counter();
        return counter == null ? 0 : counter.count();
    }
}
