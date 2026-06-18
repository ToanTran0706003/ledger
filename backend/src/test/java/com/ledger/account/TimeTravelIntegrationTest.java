package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.query.TimeTravelQueryService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Time-travel: số dư tại một thời điểm trong quá khứ dựng lại đúng từ event store. */
@SpringBootTest
@ActiveProfiles("test")
class TimeTravelIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private TimeTravelQueryService timeTravel;

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
    void balance_as_of_reflects_only_events_up_to_that_time() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));

        // Mốc thời gian theo đồng hồ DB (tránh lệch clock): thời điểm event mới nhất của alice.
        Instant cutoff = jdbc.queryForObject(
                        "SELECT max(occurred_at) FROM events WHERE aggregate_id = ?", Timestamp.class, alice)
                .toInstant();

        money.deposit(new DepositCommand(alice, new BigDecimal("500")));

        // Tại cutoff: chỉ tính lần nạp đầu.
        assertThat(timeTravel.balanceAsOf(alice, cutoff).orElseThrow()).isEqualByComparingTo("1000");
        // Hiện tại: cả hai lần nạp.
        assertThat(timeTravel.balanceAsOf(alice, Instant.now().plusSeconds(3600)).orElseThrow())
                .isEqualByComparingTo("1500");
        // Trước khi tài khoản tồn tại: không có dữ liệu.
        assertThat(timeTravel.balanceAsOf(alice, Instant.EPOCH)).isEmpty();
    }
}
