package com.ledger.account.domain;

import com.ledger.shared.domain.AbstractAggregate;
import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vòng đời và số dư một tài khoản. Số dư được suy ra từ chuỗi posting (MoneyPosted),
 * không lưu trực tiếp. Aggregate là consistency boundary: invariant "không âm" được
 * kiểm tra tại đây trước khi phát event (ADR-0005).
 */
public class AccountAggregate extends AbstractAggregate {

    private String accountId;
    private String owner;
    private AccountType type;
    private AccountStatus status;
    private BigDecimal balance = BigDecimal.ZERO;

    /** Mở một tài khoản mới (gọi trên aggregate vừa khởi tạo, chưa có lịch sử). */
    public void open(String accountId, String owner, AccountType type) {
        raise(new AccountOpened(accountId, owner, type, Instant.now()));
    }

    /** Ghi có (tăng số dư). Không có invariant chặn — tiền vào luôn hợp lệ. */
    public void credit(String txId, BigDecimal amount, MovementType movementType, String counterpartyAccountId) {
        requirePositive(amount);
        raise(new MoneyPosted(accountId, txId, Direction.CREDIT, amount, movementType, counterpartyAccountId, Instant.now()));
    }

    /** Ghi nợ (giảm số dư). Tài khoản không phải vault không được âm. */
    public void debit(String txId, BigDecimal amount, MovementType movementType, String counterpartyAccountId) {
        requirePositive(amount);
        if (type != AccountType.SYSTEM_VAULT && balance.subtract(amount).signum() < 0) {
            throw new InsufficientFundsException(accountId, balance, amount);
        }
        raise(new MoneyPosted(accountId, txId, Direction.DEBIT, amount, movementType, counterpartyAccountId, Instant.now()));
    }

    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.accountId();
                this.owner = e.owner();
                this.type = e.type();
                this.status = AccountStatus.ACTIVE;
                this.balance = BigDecimal.ZERO;
            }
            case MoneyPosted e -> this.balance = e.direction() == Direction.CREDIT
                    ? balance.add(e.amount())
                    : balance.subtract(e.amount());
            default -> throw new IllegalStateException(
                    "AccountAggregate không xử lý được event: " + event.eventType());
        }
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

    public AccountStatus status() {
        return status;
    }

    public BigDecimal balance() {
        return balance;
    }
}
