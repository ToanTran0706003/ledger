package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.AccountRepository;
import com.ledger.account.domain.AccountType;
import com.ledger.account.projection.ReadModelRebuildService;
import com.ledger.account.query.AccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test chạy trên PostgreSQL thật: command -> event store -> projector
 * -> read model -> query. Đây là bằng chứng vòng đời ES/CQRS hoạt động end-to-end.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountFlowIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ReadModelRebuildService readModelRebuild;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
    }

    @Test
    void open_account_then_balance_is_zero() {
        String accountId = openAccount.handle(new OpenAccountCommand("Alice", AccountType.CUSTOMER));

        var view = accountQuery.findBalance(accountId).orElseThrow();
        assertThat(view.owner()).isEqualTo("Alice");
        assertThat(view.balance()).isEqualByComparingTo("0");
        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.status()).isEqualTo("ACTIVE");
    }

    @Test
    void event_is_persisted_and_aggregate_replays_correctly() {
        String accountId = openAccount.handle(new OpenAccountCommand("Bob", AccountType.CUSTOMER));

        var account = accountRepository.load(accountId).orElseThrow();
        assertThat(account.owner()).isEqualTo("Bob");
        assertThat(account.type()).isEqualTo(AccountType.CUSTOMER);
        assertThat(account.version()).isEqualTo(1);
    }

    @Test
    void rebuild_read_model_reproduces_same_result() {
        String accountId = openAccount.handle(new OpenAccountCommand("Carol", AccountType.CUSTOMER));

        // Xóa read model: mô phỏng đổi schema hoặc sửa bug projection.
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        assertThat(accountQuery.findBalance(accountId)).isEmpty();

        // Dựng lại hoàn toàn từ event store -> phải ra kết quả y hệt.
        readModelRebuild.rebuild();

        var view = accountQuery.findBalance(accountId).orElseThrow();
        assertThat(view.owner()).isEqualTo("Carol");
        assertThat(view.balance()).isEqualByComparingTo("0");
        assertThat(view.status()).isEqualTo("ACTIVE");
    }
}
