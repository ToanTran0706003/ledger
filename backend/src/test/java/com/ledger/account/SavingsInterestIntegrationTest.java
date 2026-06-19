package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.interest.InterestService;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tài khoản tiết kiệm: tính lãi qua replay, lãi trả từ vault nên sổ vẫn cân. */
@SpringBootTest
@ActiveProfiles("test")
class SavingsInterestIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private InterestService interest;

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
        vaultSeed.seedIfAbsent();
    }

    @Test
    void savings_accrues_time_weighted_interest_paid_by_vault() throws InterruptedException {
        String acc = openAccount.handle(new OpenAccountCommand("Saver", AccountType.SAVINGS));
        money.deposit(new DepositCommand(acc, new BigDecimal("1000000")));
        BigDecimal before = accountQuery.findBalance(acc).orElseThrow().balance();

        Thread.sleep(100); // để một khoảng thời gian trôi (lãi suất test cao -> lãi > 0)
        BigDecimal paid = interest.accrue(acc, Instant.now());

        // Wiring: có lãi được trả, ghi vào tài khoản, đến từ vault nên sổ vẫn cân.
        assertThat(paid).isGreaterThan(BigDecimal.ZERO);
        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo(before.add(paid));
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void interest_rejected_for_non_savings_account() {
        String cust = openAccount.handle(new OpenAccountCommand("Cust", AccountType.CUSTOMER));
        assertThatThrownBy(() -> interest.accrue(cust, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
