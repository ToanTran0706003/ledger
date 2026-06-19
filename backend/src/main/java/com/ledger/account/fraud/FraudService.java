package com.ledger.account.fraud;

import com.ledger.account.command.AccountRepository;
import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Phát hiện gian lận rule-based. Sau mỗi lệnh ghi nợ, {@link #evaluate} chạy trên lịch sử gần
 * đây của tài khoản và tự đóng băng nếu vượt ngưỡng: một giao dịch lớn bất thường, hoặc quá
 * nhiều lệnh ghi nợ trong một cửa sổ thời gian. Đóng băng chỉ chặn ghi nợ TIẾP THEO (phát hiện
 * hậu kỳ, ngăn thất thoát thêm) — đúng mô hình giám sát giao dịch của ngân hàng.
 *
 * Cũng cung cấp freeze/unfreeze thủ công cho ADMIN. Tắt được qua {@code ledger.fraud.enabled}
 * (mặc định bật; tắt trong test để không nhiễu các kịch bản khác).
 */
@Service
public class FraudService {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final JdbcTemplate jdbc;

    private final boolean enabled;
    private final long windowSeconds;
    private final int maxDebitsPerWindow;
    private final BigDecimal largeAmount;

    public FraudService(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            JdbcTemplate jdbc,
            @Value("${ledger.fraud.enabled:true}") boolean enabled,
            @Value("${ledger.fraud.window-seconds:60}") long windowSeconds,
            @Value("${ledger.fraud.max-debits-per-window:10}") int maxDebitsPerWindow,
            @Value("${ledger.fraud.large-amount:100000000}") BigDecimal largeAmount) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxDebitsPerWindow = maxDebitsPerWindow;
        this.largeAmount = largeAmount;
    }

    /**
     * Đánh giá rủi ro sau một lệnh ghi nợ vừa commit (best-effort, ngoài giao dịch gốc).
     * {@code amount} là số tiền của chính lệnh vừa thực hiện. Tự đóng băng nếu chạm luật.
     * Lỗi khi đóng băng (vd đua đóng băng đồng thời, lỗi DB) được NUỐT + log: bước hậu kỳ này
     * KHÔNG được làm hỏng giao dịch đã thành công. Không cần kiểm tra trạng thái trước — tài khoản
     * đã FROZEN thì lệnh ghi nợ không tới được đây (debit đã chặn); case đua hiếm đã có nuốt lỗi.
     */
    public void evaluate(String accountId, BigDecimal amount) {
        if (!enabled) {
            return;
        }
        String reason = null;
        if (amount.compareTo(largeAmount) >= 0) {
            reason = "Giao dịch lớn bất thường: " + amount.toPlainString();
        } else {
            Timestamp since = Timestamp.from(Instant.now().minusSeconds(windowSeconds));
            int debitCount = jdbc.queryForObject(
                    """
                    SELECT count(*) FROM rm_transaction_history
                    WHERE account_id = ? AND movement_type IN ('WITHDRAWAL', 'TRANSFER') AND occurred_at >= ?
                    """,
                    Integer.class, accountId, since);
            if (debitCount >= maxDebitsPerWindow) {
                reason = "Tần suất cao: " + debitCount + " lệnh ghi nợ trong " + windowSeconds + "s";
            }
        }

        if (reason != null) {
            try {
                freeze(accountId, reason);
                log.warn("Tự đóng băng tài khoản {} do nghi ngờ gian lận: {}", accountId, reason);
            } catch (RuntimeException e) {
                log.warn("Không đóng băng được tài khoản {} (bỏ qua, giao dịch vẫn hợp lệ): {}",
                        accountId, e.getMessage());
            }
        }
    }

    /** Đóng băng tài khoản với lý do (ADMIN hoặc luật gian lận). */
    public void freeze(String accountId, String reason) {
        mutate(accountId, account -> account.freeze(reason));
    }

    /** Mở băng tài khoản (ADMIN). */
    public void unfreeze(String accountId) {
        mutate(accountId, AccountAggregate::unfreeze);
    }

    private void mutate(String accountId, java.util.function.Consumer<AccountAggregate> change) {
        executor.execute(() -> {
            AccountAggregate account =
                    repository.load(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
            change.accept(account);
            repository.append(account);
            return null;
        });
        relay.drainQuietly();
    }
}
