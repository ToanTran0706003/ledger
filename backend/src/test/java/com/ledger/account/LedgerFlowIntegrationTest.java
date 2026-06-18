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
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.account.projection.ReadModelRebuildService;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 2 end-to-end trên PostgreSQL thật: double-entry, invariant không-âm, và
 * invariant toàn cục (tổng số dư == hằng số) qua mọi nạp/rút/chuyển.
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerFlowIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private IntegrityService integrity;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private ReadModelRebuildService rebuild;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        vaultSeed.seedIfAbsent();
    }

    private String openCustomer(String owner) {
        return openAccount.handle(new OpenAccountCommand(owner, AccountType.CUSTOMER));
    }

    private BigDecimal balanceOf(String accountId) {
        return accountQuery.findBalance(accountId).orElseThrow().balance();
    }

    @Test
    void deposit_increases_balance_and_keeps_ledger_balanced() {
        String alice = openCustomer("Alice");

        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));

        assertThat(balanceOf(alice)).isEqualByComparingTo("1000");
        // Tiền đến từ vault: vault giảm đúng 1000.
        BigDecimal seed = integrity.check().expectedTotal();
        assertThat(balanceOf(SystemAccounts.VAULT_ID)).isEqualByComparingTo(seed.subtract(new BigDecimal("1000")));
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void withdraw_decreases_balance_and_keeps_ledger_balanced() {
        String alice = openCustomer("Alice");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));

        money.withdraw(new WithdrawCommand(alice, new BigDecimal("300")));

        assertThat(balanceOf(alice)).isEqualByComparingTo("700");
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void transfer_moves_money_between_customers_and_balances() {
        String alice = openCustomer("Alice");
        String bob = openCustomer("Bob");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));

        money.transfer(new TransferCommand(alice, bob, new BigDecimal("400")));

        assertThat(balanceOf(alice)).isEqualByComparingTo("600");
        assertThat(balanceOf(bob)).isEqualByComparingTo("400");
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void withdraw_beyond_balance_is_rejected_and_ledger_stays_balanced() {
        String alice = openCustomer("Alice");

        assertThatThrownBy(() -> money.withdraw(new WithdrawCommand(alice, new BigDecimal("100"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balanceOf(alice)).isEqualByComparingTo("0");
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void transfer_beyond_balance_is_rejected() {
        String alice = openCustomer("Alice");
        String bob = openCustomer("Bob");
        money.deposit(new DepositCommand(alice, new BigDecimal("100")));

        assertThatThrownBy(() -> money.transfer(new TransferCommand(alice, bob, new BigDecimal("500"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balanceOf(alice)).isEqualByComparingTo("100");
        assertThat(balanceOf(bob)).isEqualByComparingTo("0");
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void transaction_history_records_each_posting() {
        String alice = openCustomer("Alice");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));
        money.withdraw(new WithdrawCommand(alice, new BigDecimal("250")));

        var history = accountQuery.findHistory(alice);

        assertThat(history).hasSize(2);
        // Mới nhất trước: rút 250, số dư sau 750.
        assertThat(history.getFirst().direction()).isEqualTo("D");
        assertThat(history.getFirst().amount()).isEqualByComparingTo("250");
        assertThat(history.getFirst().balanceAfter()).isEqualByComparingTo("750");
    }

    @Test
    void rebuild_reproduces_balances_and_integrity() {
        String alice = openCustomer("Alice");
        String bob = openCustomer("Bob");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));
        money.transfer(new TransferCommand(alice, bob, new BigDecimal("400")));

        rebuild.rebuild();

        assertThat(balanceOf(alice)).isEqualByComparingTo("600");
        assertThat(balanceOf(bob)).isEqualByComparingTo("400");
        assertThat(integrity.check().balanced()).isTrue();
    }
}
