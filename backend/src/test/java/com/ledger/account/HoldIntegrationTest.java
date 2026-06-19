package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.command.WithdrawCommand;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.HoldReleaseReason;
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.hold.HoldQueryService;
import com.ledger.account.hold.HoldService;
import com.ledger.account.hold.HoldView;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hold/reservation end-to-end: giữ tiền làm giảm available (không giảm balance), debit tôn
 * trọng available, capture biến hold thành bút toán thật (sổ vẫn cân), release/expire nhả chỗ.
 */
@SpringBootTest
@ActiveProfiles("test")
class HoldIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private HoldService holds;

    @Autowired
    private HoldQueryService holdQuery;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private IntegrityService integrity;

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
    void placing_hold_reduces_available_not_balance() {
        String acc = funded("1000");

        holds.place(acc, new BigDecimal("300"), Duration.ofHours(1));

        var view = accountQuery.findBalance(acc).orElseThrow();
        assertThat(view.balance()).isEqualByComparingTo("1000");
        assertThat(view.available()).isEqualByComparingTo("700");
        assertThat(holdQuery.findByAccount(acc)).singleElement().satisfies(h -> {
            assertThat(h.status()).isEqualTo("ACTIVE");
            assertThat(h.amount()).isEqualByComparingTo("300");
        });
    }

    @Test
    void withdraw_is_blocked_when_it_would_exceed_available() {
        String acc = funded("1000");
        holds.place(acc, new BigDecimal("800"), Duration.ofHours(1)); // available = 200

        assertThatThrownBy(() -> money.withdraw(new WithdrawCommand(acc, new BigDecimal("500"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo("1000");
    }

    @Test
    void capturing_hold_moves_money_to_vault_and_keeps_books_balanced() {
        String acc = funded("1000");
        String holdId = holds.place(acc, new BigDecimal("300"), Duration.ofHours(1));

        holds.capture(acc, holdId);

        var view = accountQuery.findBalance(acc).orElseThrow();
        assertThat(view.balance()).isEqualByComparingTo("700");
        assertThat(view.available()).isEqualByComparingTo("700");
        assertThat(integrity.check().balanced()).isTrue();

        HoldView h = holdQuery.findByAccount(acc).getFirst();
        assertThat(h.status()).isEqualTo("CAPTURED");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM rm_transaction_history WHERE account_id = ? AND movement_type = 'CAPTURE'",
                        Integer.class,
                        acc))
                .isEqualTo(1);
    }

    @Test
    void releasing_hold_restores_available() {
        String acc = funded("1000");
        String holdId = holds.place(acc, new BigDecimal("300"), Duration.ofHours(1));

        holds.release(acc, holdId, HoldReleaseReason.MANUAL);

        var view = accountQuery.findBalance(acc).orElseThrow();
        assertThat(view.balance()).isEqualByComparingTo("1000");
        assertThat(view.available()).isEqualByComparingTo("1000");
        assertThat(holdQuery.findByAccount(acc).getFirst().status()).isEqualTo("RELEASED");
    }

    @Test
    void expired_holds_are_released_by_the_sweeper() {
        String acc = funded("1000");
        holds.place(acc, new BigDecimal("300"), Duration.ofSeconds(1));

        int released = holds.expireDue(Instant.now().plusSeconds(3600));

        assertThat(released).isEqualTo(1);
        assertThat(accountQuery.findBalance(acc).orElseThrow().available()).isEqualByComparingTo("1000");
        HoldView h = holdQuery.findByAccount(acc).getFirst();
        assertThat(h.status()).isEqualTo("RELEASED");
        assertThat(h.reason()).isEqualTo("EXPIRED");
    }

    private String funded(String amount) {
        String acc = openAccount.handle(new OpenAccountCommand("Holder", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal(amount)));
        return acc;
    }
}
