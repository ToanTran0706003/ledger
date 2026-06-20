package com.ledger.account.hold;

import com.ledger.account.command.AccountRepository;
import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.HoldReleaseReason;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Đặt giữ (hold) / nhả / thu (capture) tiền. Mỗi thao tác chạy trong một transaction qua
 * RetryingTransactionExecutor (retry khi optimistic conflict) rồi drain outbox để read model
 * cập nhật ngay (read-your-writes) — cùng khuôn với MoneyMovementHandler (ADR-0006).
 *
 * Capture ghi sổ kép: nợ tài khoản, có SYSTEM_VAULT (tiền rời hệ thống như một thanh toán),
 * giữ integrity tổng tiền không đổi.
 */
@Service
public class HoldService {

    private static final Logger log = LoggerFactory.getLogger(HoldService.class);

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final HoldQueryService holdQuery;

    public HoldService(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            HoldQueryService holdQuery) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.holdQuery = holdQuery;
    }

    /** Đặt giữ {@code amount} trên tài khoản, hết hạn sau {@code ttl}. Trả về holdId. */
    public String place(String accountId, BigDecimal amount, Duration ttl) {
        String holdId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        return run(() -> {
            AccountAggregate account = load(accountId);
            account.placeHold(holdId, amount, expiresAt);
            repository.append(account);
            return holdId;
        });
    }

    /** Nhả một hold (trả lại available, balance không đổi). */
    public void release(String accountId, String holdId, HoldReleaseReason reason) {
        run(() -> {
            AccountAggregate account = load(accountId);
            account.releaseHold(holdId, reason);
            repository.append(account);
            return holdId;
        });
    }

    /** Thu một hold: nhả phần giữ rồi ghi sổ kép (nợ tài khoản, có vault). Trả về txId. */
    public String capture(String accountId, String holdId) {
        String txId = UUID.randomUUID().toString();
        return run(() -> {
            AccountAggregate account = load(accountId);
            BigDecimal amount = account.holdAmount(holdId);
            String vaultId = SystemAccounts.vaultFor(account.currency());
            account.captureHold(holdId); // nhả chỗ -> available khôi phục, debit ngay sau kiểm tra đúng
            account.debit(txId, amount, MovementType.CAPTURE, vaultId);
            repository.append(account);

            AccountAggregate vault = load(vaultId);
            vault.credit(txId, amount, MovementType.CAPTURE, accountId);
            repository.append(vault);
            return txId;
        });
    }

    /** Tự nhả mọi hold đã quá hạn; trả về số hold đã nhả. Lỗi một hold không chặn các hold khác. */
    public int expireDue(Instant now) {
        int released = 0;
        for (HoldQueryService.DueHold due : holdQuery.findDue(now)) {
            try {
                release(due.accountId(), due.holdId(), HoldReleaseReason.EXPIRED);
                released++;
            } catch (RuntimeException e) {
                // Hold có thể vừa được nhả/capture bởi luồng khác — bỏ qua, lần quét sau sẽ ổn.
                log.info("Bỏ qua nhả hold hết hạn {}: {}", due.holdId(), e.getMessage());
            }
        }
        return released;
    }

    private <T> T run(Supplier<T> action) {
        T result = executor.execute(action);
        relay.drainQuietly();
        return result;
    }

    private AccountAggregate load(String accountId) {
        return repository.load(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
