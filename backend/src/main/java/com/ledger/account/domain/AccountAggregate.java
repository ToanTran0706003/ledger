package com.ledger.account.domain;

import com.ledger.shared.domain.AbstractAggregate;
import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;

/**
 * Vòng đời một tài khoản. Phase 1 (walking skeleton) chỉ có thao tác mở tài khoản;
 * deposit/withdraw/transfer và lifecycle FREEZE/CLOSE sẽ thêm ở các phase sau.
 */
public class AccountAggregate extends AbstractAggregate {

    private String accountId;
    private String owner;
    private AccountType type;
    private AccountStatus status;

    /** Mở một tài khoản mới (gọi trên aggregate vừa khởi tạo, chưa có lịch sử). */
    public void open(String accountId, String owner, AccountType type) {
        raise(new AccountOpened(accountId, owner, type, Instant.now()));
    }

    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.accountId();
                this.owner = e.owner();
                this.type = e.type();
                this.status = AccountStatus.ACTIVE;
            }
            default -> throw new IllegalStateException(
                    "AccountAggregate không xử lý được event: " + event.eventType());
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
}
