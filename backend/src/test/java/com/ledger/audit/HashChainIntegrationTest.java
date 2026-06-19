package com.ledger.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.audit.query.HashChainReport;
import com.ledger.audit.query.HashChainVerifier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hash-chain chống giả mạo: mỗi event mang hash = SHA-256 của (hash trước + nội dung).
 * Sửa một event đã ghi sẽ làm gãy chuỗi và verify phát hiện được đúng chỗ gãy.
 */
@SpringBootTest
@ActiveProfiles("test")
class HashChainIntegrationTest {

    private static final String GENESIS = "0".repeat(64);

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private HashChainVerifier verifier;

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
        jdbc.update("TRUNCATE TABLE rm_hold");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void every_event_is_hashed_and_first_per_aggregate_links_to_genesis() {
        String acc = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal("500")));

        Integer nullHashes = jdbc.queryForObject(
                "SELECT count(*) FROM events WHERE hash IS NULL OR prev_hash IS NULL", Integer.class);
        assertThat(nullHashes).isZero();

        String firstPrev = jdbc.queryForObject(
                "SELECT prev_hash FROM events WHERE aggregate_id = ? ORDER BY aggregate_version LIMIT 1",
                String.class, acc);
        assertThat(firstPrev).isEqualTo(GENESIS);
    }

    @Test
    void intact_chain_verifies_clean() {
        String acc = openAccount.handle(new OpenAccountCommand("Bob", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal("1000")));
        money.deposit(new DepositCommand(acc, new BigDecimal("250")));

        HashChainReport report = verifier.verify();

        assertThat(report.intact()).isTrue();
        assertThat(report.eventsChecked()).isGreaterThan(0);
        assertThat(report.firstBrokenSeq()).isNull();
    }

    @Test
    void tampering_a_stored_event_breaks_the_chain_and_is_detected() {
        String acc = openAccount.handle(new OpenAccountCommand("Eve", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal("1000")));

        // Giả mạo: sửa payload một event đã ghi (điều mà event store append-only cấm).
        Long seq = jdbc.queryForObject(
                "SELECT global_seq FROM events WHERE aggregate_id = ? ORDER BY global_seq LIMIT 1",
                Long.class, acc);
        jdbc.update(
                "UPDATE events SET payload = payload || '{\"tampered\":true}'::jsonb WHERE global_seq = ?", seq);

        HashChainReport report = verifier.verify();

        assertThat(report.intact()).isFalse();
        assertThat(report.firstBrokenSeq()).isEqualTo(seq);
    }

    @Test
    void deleting_a_middle_event_breaks_the_link_to_the_next_event() {
        String acc = openAccount.handle(new OpenAccountCommand("Mallory", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal("100"))); // version 2
        money.deposit(new DepositCommand(acc, new BigDecimal("200"))); // version 3

        // Các event của acc theo version: v1 (mở), v2, v3. Xoá v2 (giữa chuỗi).
        List<Long> seqs = jdbc.queryForList(
                "SELECT global_seq FROM events WHERE aggregate_id = ? ORDER BY aggregate_version", Long.class, acc);
        jdbc.update("DELETE FROM events WHERE global_seq = ?", seqs.get(1));

        // v3 còn nguyên (hash tự khớp) nhưng prev_hash của nó trỏ tới v2 đã mất -> gãy liên kết.
        HashChainReport report = verifier.verify();
        assertThat(report.intact()).isFalse();
        assertThat(report.firstBrokenSeq()).isEqualTo(seqs.get(2));
    }
}
