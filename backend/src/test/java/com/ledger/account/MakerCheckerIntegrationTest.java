package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.approval.ApprovalStatus;
import com.ledger.account.approval.MakerCheckerService;
import com.ledger.account.approval.SelfApprovalException;
import com.ledger.account.approval.TransferResult;
import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
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
 * Maker-checker (nguyên tắc bốn-mắt): chuyển tiền vượt ngưỡng KHÔNG thực thi ngay mà tạo yêu cầu
 * chờ duyệt; một người DUYỆT KHÁC người tạo mới được phê duyệt để thực thi. Dưới ngưỡng chạy bình
 * thường. Mặc định tắt trong test; bật + ngưỡng thấp qua @TestPropertySource.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"ledger.maker-checker.enabled=true", "ledger.maker-checker.threshold=1000000"})
class MakerCheckerIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private MakerCheckerService makerChecker;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    private String a;
    private String b;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        jdbc.update("TRUNCATE TABLE rm_hold");
        jdbc.update("TRUNCATE TABLE pending_transfers");
        vaultSeed.seedIfAbsent();
        a = openAccount.handle(new OpenAccountCommand("maker", AccountType.CUSTOMER));
        b = openAccount.handle(new OpenAccountCommand("other", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(a, new BigDecimal("5000000")));
    }

    @Test
    void transfer_below_threshold_executes_immediately() {
        TransferResult r = makerChecker.submitTransfer("maker", a, b, new BigDecimal("500000"));

        assertThat(r.status()).isEqualTo("EXECUTED");
        assertThat(r.txId()).isNotNull();
        assertThat(accountQuery.findBalance(b).orElseThrow().balance()).isEqualByComparingTo("500000");
    }

    @Test
    void transfer_over_threshold_is_held_for_approval_then_executes_on_approval_by_someone_else() {
        TransferResult r = makerChecker.submitTransfer("maker", a, b, new BigDecimal("2000000"));
        assertThat(r.status()).isEqualTo("PENDING_APPROVAL");
        // Chưa thực thi: số dư không đổi.
        assertThat(accountQuery.findBalance(a).orElseThrow().balance()).isEqualByComparingTo("5000000");
        assertThat(makerChecker.listPending()).hasSize(1);

        // Người tạo KHÔNG được tự duyệt (four-eyes).
        assertThatThrownBy(() -> makerChecker.approve("maker", r.approvalId()))
                .isInstanceOf(SelfApprovalException.class);

        // Người duyệt khác → thực thi.
        makerChecker.approve("admin", r.approvalId());
        assertThat(accountQuery.findBalance(a).orElseThrow().balance()).isEqualByComparingTo("3000000");
        assertThat(accountQuery.findBalance(b).orElseThrow().balance()).isEqualByComparingTo("2000000");
        assertThat(makerChecker.listPending()).isEmpty();
    }

    @Test
    void rejected_request_does_not_execute() {
        TransferResult r = makerChecker.submitTransfer("maker", a, b, new BigDecimal("2000000"));

        makerChecker.reject("admin", r.approvalId(), "không hợp lệ");

        assertThat(accountQuery.findBalance(a).orElseThrow().balance()).isEqualByComparingTo("5000000");
        assertThat(makerChecker.listPending()).isEmpty();
        assertThat(makerChecker.find(r.approvalId()).getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }
}
