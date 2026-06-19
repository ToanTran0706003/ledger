package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.query.AccountQueryService;
import com.ledger.account.standingorder.StandingOrder;
import com.ledger.account.standingorder.StandingOrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Chuyển tiền định kỳ: lệnh đến hạn được thực thi và dời sang chu kỳ kế. */
@SpringBootTest
@ActiveProfiles("test")
class StandingOrderIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private StandingOrderService standingOrders;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE standing_orders");
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        vaultSeed.seedIfAbsent();
    }

    private String openCustomer(String owner) {
        return openAccount.handle(new OpenAccountCommand(owner, AccountType.CUSTOMER));
    }

    private BigDecimal balanceOf(String id) {
        return accountQuery.findBalance(id).orElseThrow().balance();
    }

    @Test
    void due_order_executes_transfer_and_advances() {
        String a = openCustomer("alice");
        String b = openCustomer("bob");
        money.deposit(new DepositCommand(a, new BigDecimal("1000")));
        standingOrders.create("alice", a, b, new BigDecimal("300"), 86400);

        int executed = standingOrders.runDue(Instant.now().plusSeconds(5));

        assertThat(executed).isEqualTo(1);
        assertThat(balanceOf(a)).isEqualByComparingTo("700");
        assertThat(balanceOf(b)).isEqualByComparingTo("300");

        List<StandingOrder> orders = standingOrders.listFor("alice");
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().isActive()).isTrue();
        assertThat(orders.getFirst().getNextRunAt()).isAfter(Instant.now());
    }

    @Test
    void insufficient_funds_order_is_skipped_but_stays_active() {
        String a = openCustomer("alice"); // số dư 0
        String b = openCustomer("bob");
        standingOrders.create("alice", a, b, new BigDecimal("300"), 86400);

        int executed = standingOrders.runDue(Instant.now().plusSeconds(5));

        assertThat(executed).isZero();
        assertThat(balanceOf(a)).isEqualByComparingTo("0");
        assertThat(standingOrders.listFor("alice").getFirst().isActive()).isTrue();
    }
}
