package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.command.WithdrawCommand;
import com.ledger.account.domain.AccountFrozenException;
import com.ledger.account.domain.AccountType;
import com.ledger.account.fraud.FraudService;
import com.ledger.account.query.AccountQueryService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Phát hiện gian lận rule-based: sau mỗi lệnh ghi nợ, luật chạy trên lịch sử gần đây và
 * tự đóng băng tài khoản nếu vượt ngưỡng (giao dịch lớn bất thường hoặc tần suất cao).
 * Ngưỡng đặt thấp ở đây để quan sát; mặc định fraud TẮT trong profile test.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "ledger.fraud.enabled=true",
            "ledger.fraud.window-seconds=3600",
            "ledger.fraud.max-debits-per-window=3",
            "ledger.fraud.large-amount=500"
        })
class FraudDetectionIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private FraudService fraud;

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
        jdbc.update("TRUNCATE TABLE rm_hold");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void large_single_debit_auto_freezes_the_account() {
        String acc = funded("10000");

        money.withdraw(new WithdrawCommand(acc, new BigDecimal("600"))); // 600 >= ngưỡng 500

        var view = accountQuery.findBalance(acc).orElseThrow();
        assertThat(view.status()).isEqualTo("FROZEN");
        // Tài khoản đã đóng băng: lệnh ghi nợ tiếp theo bị từ chối.
        assertThatThrownBy(() -> money.withdraw(new WithdrawCommand(acc, new BigDecimal("10"))))
                .isInstanceOf(AccountFrozenException.class);
    }

    @Test
    void high_velocity_auto_freezes_the_account() {
        String acc = funded("10000");

        // 3 lệnh nhỏ trong cửa sổ -> chạm ngưỡng tần suất (max 3).
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("10")));
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("10")));
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("10")));

        assertThat(accountQuery.findBalance(acc).orElseThrow().status()).isEqualTo("FROZEN");
    }

    @Test
    void normal_activity_does_not_freeze() {
        String acc = funded("10000");

        money.withdraw(new WithdrawCommand(acc, new BigDecimal("100")));

        assertThat(accountQuery.findBalance(acc).orElseThrow().status()).isEqualTo("ACTIVE");
    }

    @Test
    void admin_can_unfreeze_to_restore_debits() {
        String acc = funded("10000");
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("600"))); // tự đóng băng
        assertThat(accountQuery.findBalance(acc).orElseThrow().status()).isEqualTo("FROZEN");

        fraud.unfreeze(acc);

        assertThat(accountQuery.findBalance(acc).orElseThrow().status()).isEqualTo("ACTIVE");
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("100"))); // lại ghi nợ được
        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo("9300");
    }

    private String funded(String amount) {
        String acc = openAccount.handle(new OpenAccountCommand("Trader", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal(amount)));
        return acc;
    }
}
