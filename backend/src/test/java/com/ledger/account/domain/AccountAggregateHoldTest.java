package com.ledger.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Hold/reservation tại tầng aggregate: giữ chỗ tiền làm giảm "available" nhưng không
 * giảm "balance". Invariant lõi đổi: debit phải tôn trọng available = balance - Σ(hold),
 * không chỉ balance. Capture biến hold thành bút toán thật; release/expire nhả chỗ.
 */
class AccountAggregateHoldTest {

    private static final Instant EXPIRES = Instant.now().plusSeconds(3600);

    @Test
    void place_hold_reduces_available_not_balance() {
        AccountAggregate account = funded("acc-1", "1000");

        account.placeHold("h1", new BigDecimal("300"), EXPIRES);

        assertThat(account.balance()).isEqualByComparingTo("1000");
        assertThat(account.available()).isEqualByComparingTo("700");
    }

    @Test
    void debit_is_blocked_when_amount_exceeds_available_even_if_balance_suffices() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("800"), EXPIRES); // available = 200

        assertThatThrownBy(
                        () -> account.debit("tx", new BigDecimal("500"), MovementType.WITHDRAWAL, SystemAccounts.VAULT_ID))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.balance()).isEqualByComparingTo("1000");
        assertThat(account.available()).isEqualByComparingTo("200");
    }

    @Test
    void debit_within_available_succeeds() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("800"), EXPIRES); // available = 200

        account.debit("tx", new BigDecimal("200"), MovementType.WITHDRAWAL, SystemAccounts.VAULT_ID);

        assertThat(account.balance()).isEqualByComparingTo("800");
        assertThat(account.available()).isEqualByComparingTo("0");
    }

    @Test
    void cannot_place_hold_exceeding_available() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("700"), EXPIRES); // available = 300

        assertThatThrownBy(() -> account.placeHold("h2", new BigDecimal("400"), EXPIRES))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void release_restores_available_without_touching_balance() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("300"), EXPIRES);

        account.releaseHold("h1", HoldReleaseReason.MANUAL);

        assertThat(account.balance()).isEqualByComparingTo("1000");
        assertThat(account.available()).isEqualByComparingTo("1000");
    }

    @Test
    void capture_releases_hold_with_captured_reason_and_restores_available_for_the_debit() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("300"), EXPIRES);
        account.markEventsCommitted();

        account.captureHold("h1");

        // Hold được nhả (CAPTURED) -> available khôi phục về 1000; vế nợ thật do service ghi sau.
        List<DomainEvent> events = account.uncommittedEvents();
        assertThat(events).singleElement().isInstanceOf(HoldReleased.class);
        HoldReleased released = (HoldReleased) events.getFirst();
        assertThat(released.reason()).isEqualTo(HoldReleaseReason.CAPTURED);
        assertThat(released.amount()).isEqualByComparingTo("300");
        assertThat(account.available()).isEqualByComparingTo("1000");

        // Service ghi nợ ngay sau đó: invariant tính trên available đã khôi phục.
        account.debit("cap-tx", new BigDecimal("300"), MovementType.CAPTURE, SystemAccounts.VAULT_ID);
        assertThat(account.balance()).isEqualByComparingTo("700");
        assertThat(account.available()).isEqualByComparingTo("700");
    }

    @Test
    void releasing_unknown_hold_fails() {
        AccountAggregate account = funded("acc-1", "1000");

        assertThatThrownBy(() -> account.releaseHold("nope", HoldReleaseReason.MANUAL))
                .isInstanceOf(HoldNotFoundException.class);
    }

    @Test
    void cannot_place_two_holds_with_same_id() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("100"), EXPIRES);

        assertThatThrownBy(() -> account.placeHold("h1", new BigDecimal("100"), EXPIRES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hold_survives_snapshot_round_trip() {
        AccountAggregate account = funded("acc-1", "1000");
        account.placeHold("h1", new BigDecimal("300"), EXPIRES);
        int version = account.version() + account.uncommittedEvents().size();

        AccountSnapshot snapshot = account.toSnapshot();
        AccountAggregate restored = new AccountAggregate();
        restored.restoreFromSnapshot(snapshot, version);

        // Hold phải còn sau khi khôi phục từ snapshot, nếu không invariant sẽ vỡ.
        assertThat(restored.available()).isEqualByComparingTo("700");
        assertThat(restored.balance()).isEqualByComparingTo("1000");
    }

    private static AccountAggregate funded(String id, String amount) {
        AccountAggregate account = new AccountAggregate();
        account.open(id, "owner-" + id, AccountType.CUSTOMER);
        account.credit("seed", new BigDecimal(amount), MovementType.DEPOSIT, SystemAccounts.VAULT_ID);
        return account;
    }
}
