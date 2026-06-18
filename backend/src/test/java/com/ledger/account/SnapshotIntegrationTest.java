package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.AccountRepository;
import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Snapshot tăng tốc load mà KHÔNG đổi kết quả: load qua snapshot + replay phần sau phải
 * ra số dư y hệt load bằng replay toàn bộ. (Test profile: every-n-events = 3.)
 */
@SpringBootTest
@ActiveProfiles("test")
class SnapshotIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private AccountRepository repository;

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
    void snapshot_is_written_after_n_events_and_load_matches_full_replay() {
        String alice = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER)); // version 1
        money.deposit(new DepositCommand(alice, new BigDecimal("100"))); // version 2
        money.deposit(new DepositCommand(alice, new BigDecimal("100"))); // version 3 -> snapshot

        Integer snapVersion = jdbc.queryForObject(
                "SELECT aggregate_version FROM snapshots WHERE aggregate_id = ?", Integer.class, alice);
        assertThat(snapVersion).isEqualTo(3);
        assertThat(repository.load(alice).orElseThrow().balance()).isEqualByComparingTo("200");

        // Thêm event sau snapshot: load = snapshot(v3) + replay event v4.
        money.deposit(new DepositCommand(alice, new BigDecimal("100"))); // version 4
        assertThat(repository.load(alice).orElseThrow().balance()).isEqualByComparingTo("300");

        // Xóa snapshot -> load bằng replay toàn bộ phải ra kết quả y hệt.
        jdbc.update("DELETE FROM snapshots WHERE aggregate_id = ?", alice);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM snapshots WHERE aggregate_id = ?", Integer.class, alice))
                .isZero();
        assertThat(repository.load(alice).orElseThrow().balance()).isEqualByComparingTo("300");
    }
}
