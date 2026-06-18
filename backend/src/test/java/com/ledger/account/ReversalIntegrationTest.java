package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.ReverseTransactionCommand;
import com.ledger.account.command.ReverseTransactionHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Reversal: bù giao dịch bằng bút toán ngược (không xóa lịch sử), sổ vẫn cân. */
@SpringBootTest
@ActiveProfiles("test")
class ReversalIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private ReverseTransactionHandler reverseHandler;

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

    private String open(String owner) {
        return openAccount.handle(new OpenAccountCommand(owner, AccountType.CUSTOMER));
    }

    private BigDecimal balanceOf(String id) {
        return accountQuery.findBalance(id).orElseThrow().balance();
    }

    @Test
    void reverse_restores_balances_and_keeps_history() {
        String alice = open("Alice");
        String bob = open("Bob");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));
        String txId = money.transfer(new TransferCommand(alice, bob, new BigDecimal("400")));
        assertThat(balanceOf(alice)).isEqualByComparingTo("600");
        assertThat(balanceOf(bob)).isEqualByComparingTo("400");

        reverseHandler.reverse(new ReverseTransactionCommand(txId));

        assertThat(balanceOf(alice)).isEqualByComparingTo("1000");
        assertThat(balanceOf(bob)).isEqualByComparingTo("0");
        assertThat(integrity.check().balanced()).isTrue();

        // Lịch sử giữ nguyên: alice có nạp(C) + chuyển đi(D) + bù(C) = 3 dòng.
        assertThat(accountQuery.findHistory(alice)).hasSize(3);
        Integer reversalRows = jdbc.queryForObject(
                "SELECT count(*) FROM rm_transaction_history WHERE movement_type = 'REVERSAL'", Integer.class);
        assertThat(reversalRows).isEqualTo(2); // một vế cho mỗi tài khoản
    }

    @Test
    void reverse_fails_if_funds_already_moved_on() {
        String alice = open("Alice");
        String bob = open("Bob");
        String carol = open("Carol");
        money.deposit(new DepositCommand(alice, new BigDecimal("1000")));
        String txId = money.transfer(new TransferCommand(alice, bob, new BigDecimal("1000")));
        money.transfer(new TransferCommand(bob, carol, new BigDecimal("1000"))); // bob tiêu hết

        // Đảo giao dịch alice->bob đòi debit bob 1000 nhưng bob đã hết tiền.
        assertThatThrownBy(() -> reverseHandler.reverse(new ReverseTransactionCommand(txId)))
                .isInstanceOf(InsufficientFundsException.class);

        // Không có gì thay đổi, sổ vẫn cân.
        assertThat(balanceOf(alice)).isEqualByComparingTo("0");
        assertThat(balanceOf(bob)).isEqualByComparingTo("0");
        assertThat(balanceOf(carol)).isEqualByComparingTo("1000");
        assertThat(integrity.check().balanced()).isTrue();
    }
}
