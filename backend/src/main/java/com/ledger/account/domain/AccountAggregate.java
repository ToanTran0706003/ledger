package com.ledger.account.domain;

import com.ledger.shared.domain.AbstractAggregate;
import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vòng đời và số dư một tài khoản. Số dư được suy ra từ chuỗi posting (MoneyPosted),
 * không lưu trực tiếp. Aggregate là consistency boundary: invariant "không âm" được
 * kiểm tra tại đây trước khi phát event (ADR-0005). Có thể khôi phục từ snapshot rồi
 * replay phần event sau đó (ADR-0008).
 */
public class AccountAggregate extends AbstractAggregate {

    private String accountId;
    private String owner;
    private AccountType type;
    private String currency;
    private AccountStatus status;
    private BigDecimal balance = BigDecimal.ZERO;
    // holdId -> số tiền đang giữ. Available = balance - Σ(hold). Dùng LinkedHashMap để thứ tự ổn định.
    private final Map<String, BigDecimal> holds = new LinkedHashMap<>();

    /** Mở một tài khoản VND (mặc định). */
    public void open(String accountId, String owner, AccountType type) {
        open(accountId, owner, type, SystemAccounts.DEFAULT_CURRENCY);
    }

    /** Mở một tài khoản với tiền tệ chỉ định (gọi trên aggregate vừa khởi tạo, chưa có lịch sử). */
    public void open(String accountId, String owner, AccountType type, String currency) {
        raise(new AccountOpened(accountId, owner, type, currency, Instant.now()));
    }

    /** Ghi có (tăng số dư). Không có invariant chặn — tiền vào luôn hợp lệ. */
    public void credit(String txId, BigDecimal amount, MovementType movementType, String counterpartyAccountId) {
        credit(txId, amount, movementType, counterpartyAccountId, null);
    }

    public void credit(
            String txId,
            BigDecimal amount,
            MovementType movementType,
            String counterpartyAccountId,
            String reversalOfTxId) {
        requirePositive(amount);
        raise(new MoneyPosted(
                accountId, txId, Direction.CREDIT, amount, movementType, counterpartyAccountId, Instant.now(),
                reversalOfTxId));
    }

    /** Ghi nợ (giảm số dư). Tài khoản không phải vault không được âm. */
    public void debit(String txId, BigDecimal amount, MovementType movementType, String counterpartyAccountId) {
        debit(txId, amount, movementType, counterpartyAccountId, null);
    }

    public void debit(
            String txId,
            BigDecimal amount,
            MovementType movementType,
            String counterpartyAccountId,
            String reversalOfTxId) {
        requirePositive(amount);
        // Tài khoản bị đóng băng không được để tiền chảy ra (ghi có vẫn cho phép).
        if (status == AccountStatus.FROZEN) {
            throw new AccountFrozenException(accountId);
        }
        // Invariant lõi: ghi nợ phải tôn trọng số dư KHẢ DỤNG (đã trừ hold), không chỉ balance.
        if (type != AccountType.SYSTEM_VAULT && available().subtract(amount).signum() < 0) {
            throw new InsufficientFundsException(accountId, available(), amount);
        }
        raise(new MoneyPosted(
                accountId, txId, Direction.DEBIT, amount, movementType, counterpartyAccountId, Instant.now(),
                reversalOfTxId));
    }

    /** Đặt giữ một phần available; số dư không đổi nhưng available giảm. Hold sẽ hết hạn tại {@code expiresAt}. */
    public void placeHold(String holdId, BigDecimal amount, Instant expiresAt) {
        requirePositive(amount);
        if (holds.containsKey(holdId)) {
            throw new IllegalArgumentException("Hold đã tồn tại: " + holdId);
        }
        if (type != AccountType.SYSTEM_VAULT && available().subtract(amount).signum() < 0) {
            throw new InsufficientFundsException(accountId, available(), amount);
        }
        raise(new HoldPlaced(accountId, holdId, amount, Instant.now(), expiresAt));
    }

    /** Nhả một hold (không capture): trả lại available, balance không đổi. */
    public void releaseHold(String holdId, HoldReleaseReason reason) {
        BigDecimal held = requireActiveHold(holdId);
        raise(new HoldReleased(accountId, holdId, held, reason, Instant.now()));
    }

    /**
     * Thu (capture) một hold: nhả phần giữ với lý do CAPTURED. Bút toán nợ thật (và vế có
     * đối ứng) do tầng service ghi ngay sau đó qua {@link #debit} — giữ cả hai vế của một
     * lần di chuyển tiền cùng một chỗ, đối xứng với luồng chuyển tiền thường.
     */
    public void captureHold(String holdId) {
        BigDecimal held = requireActiveHold(holdId);
        raise(new HoldReleased(accountId, holdId, held, HoldReleaseReason.CAPTURED, Instant.now()));
    }

    /** Số tiền đang giữ của một hold đang hoạt động (ném HoldNotFoundException nếu không có). */
    public BigDecimal holdAmount(String holdId) {
        return requireActiveHold(holdId);
    }

    /** Đóng băng tài khoản (kiểm soát rủi ro/gian lận). Không cho đóng băng hai lần. */
    public void freeze(String reason) {
        if (status == AccountStatus.FROZEN) {
            throw new AccountStateConflictException("Tài khoản đã bị đóng băng");
        }
        raise(new AccountFrozen(accountId, reason, Instant.now()));
    }

    /** Mở băng tài khoản đang bị đóng băng. */
    public void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new AccountStateConflictException("Tài khoản không bị đóng băng");
        }
        raise(new AccountUnfrozen(accountId, Instant.now()));
    }

    private BigDecimal requireActiveHold(String holdId) {
        BigDecimal held = holds.get(holdId);
        if (held == null) {
            throw new HoldNotFoundException(accountId, holdId);
        }
        return held;
    }

    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.accountId();
                this.owner = e.owner();
                this.type = e.type();
                this.currency = e.currency() != null ? e.currency() : SystemAccounts.DEFAULT_CURRENCY;
                this.status = AccountStatus.ACTIVE;
                this.balance = BigDecimal.ZERO;
            }
            case MoneyPosted e -> this.balance = e.direction() == Direction.CREDIT
                    ? balance.add(e.amount())
                    : balance.subtract(e.amount());
            case HoldPlaced e -> holds.put(e.holdId(), e.amount());
            case HoldReleased e -> holds.remove(e.holdId());
            case AccountFrozen e -> this.status = AccountStatus.FROZEN;
            case AccountUnfrozen e -> this.status = AccountStatus.ACTIVE;
            default -> throw new IllegalStateException(
                    "AccountAggregate không xử lý được event: " + event.eventType());
        }
    }

    /** Ảnh chụp trạng thái hiện tại để lưu snapshot. */
    public AccountSnapshot toSnapshot() {
        return new AccountSnapshot(accountId, owner, type, currency, status, balance, new LinkedHashMap<>(holds));
    }

    /** Khôi phục trạng thái từ snapshot tại một version; sau đó replay event mới hơn. */
    public void restoreFromSnapshot(AccountSnapshot snapshot, int version) {
        this.accountId = snapshot.accountId();
        this.owner = snapshot.owner();
        this.type = snapshot.type();
        this.currency = snapshot.currency() != null ? snapshot.currency() : SystemAccounts.DEFAULT_CURRENCY;
        this.status = snapshot.status();
        this.balance = snapshot.balance();
        this.holds.clear();
        if (snapshot.holds() != null) {
            this.holds.putAll(snapshot.holds());
        }
        restoreVersion(version);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
    }

    public String accountId() {
        return accountId;
    }

    public String owner() {
        return owner;
    }

    public AccountType type() {
        return type;
    }

    public String currency() {
        return currency;
    }

    public AccountStatus status() {
        return status;
    }

    public BigDecimal balance() {
        return balance;
    }

    /** Số dư khả dụng = balance - tổng các hold đang giữ. */
    public BigDecimal available() {
        return balance.subtract(totalHeld());
    }

    private BigDecimal totalHeld() {
        return holds.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
