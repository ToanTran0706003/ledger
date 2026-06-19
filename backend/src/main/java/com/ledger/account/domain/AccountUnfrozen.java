package com.ledger.account.domain;

import com.ledger.shared.domain.DomainEvent;
import java.time.Instant;

/** Tài khoản được mở băng trở lại trạng thái hoạt động; ghi nợ được phép trở lại. */
public record AccountUnfrozen(String accountId, Instant unfrozenAt) implements DomainEvent {
}
