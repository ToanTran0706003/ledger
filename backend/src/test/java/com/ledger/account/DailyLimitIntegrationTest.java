package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.command.WithdrawCommand;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.DailyLimitExceededException;
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
 * Hạn mức ghi nợ theo ngày: tổng ghi nợ (rút + chuyển) trong ngày của một tài khoản không
 * vượt ngưỡng. Kiểm tra TRONG transaction trên event đã commit; nhờ optimistic concurrency
 * theo aggregate, hai lệnh cùng tài khoản bị serialize nên hạn mức chính xác cả khi đồng thời.
 * Mặc định tắt trong profile test; bật + ngưỡng thấp qua @TestPropertySource.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"ledger.daily-limit.enabled=true", "ledger.daily-limit.max-per-day=1000"})
class DailyLimitIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

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
    void debits_within_the_daily_limit_succeed() {
        String acc = funded("10000");

        money.withdraw(new WithdrawCommand(acc, new BigDecimal("600")));

        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo("9400");
    }

    @Test
    void debit_pushing_over_the_daily_limit_is_rejected() {
        String acc = funded("10000");
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("600"))); // tổng hôm nay = 600

        // 600 + 500 = 1100 > 1000 -> từ chối; số dư không đổi.
        assertThatThrownBy(() -> money.withdraw(new WithdrawCommand(acc, new BigDecimal("500"))))
                .isInstanceOf(DailyLimitExceededException.class);
        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo("9400");
    }

    @Test
    void transfers_count_toward_the_same_daily_limit() {
        String acc = funded("10000");
        String other = openAccount.handle(new OpenAccountCommand("Other", AccountType.CUSTOMER));
        money.withdraw(new WithdrawCommand(acc, new BigDecimal("700")));

        assertThatThrownBy(() -> money.transfer(new TransferCommand(acc, other, new BigDecimal("400"))))
                .isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    void deposits_are_not_limited() {
        String acc = funded("10000");

        money.deposit(new DepositCommand(acc, new BigDecimal("50000")));

        assertThat(accountQuery.findBalance(acc).orElseThrow().balance()).isEqualByComparingTo("60000");
    }

    private String funded(String amount) {
        String acc = openAccount.handle(new OpenAccountCommand("Spender", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal(amount)));
        return acc;
    }
}
