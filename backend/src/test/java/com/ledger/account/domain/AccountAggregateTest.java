package com.ledger.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountAggregateTest {

    @Test
    void open_raises_AccountOpened_event() {
        AccountAggregate account = new AccountAggregate();

        account.open("acc-1", "Alice", AccountType.CUSTOMER);

        List<DomainEvent> events = account.uncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(AccountOpened.class);

        AccountOpened opened = (AccountOpened) events.getFirst();
        assertThat(opened.accountId()).isEqualTo("acc-1");
        assertThat(opened.owner()).isEqualTo("Alice");
        assertThat(opened.type()).isEqualTo(AccountType.CUSTOMER);
        assertThat(opened.openedAt()).isNotNull();
    }

    @Test
    void open_sets_state_to_active_with_zero_balance() {
        AccountAggregate account = new AccountAggregate();

        account.open("acc-1", "Alice", AccountType.CUSTOMER);

        assertThat(account.accountId()).isEqualTo("acc-1");
        assertThat(account.owner()).isEqualTo("Alice");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance()).isEqualByComparingTo("0");
    }

    @Test
    void credit_then_debit_updates_balance() {
        AccountAggregate account = opened("acc-1", AccountType.CUSTOMER);

        account.credit("tx1", new BigDecimal("1000"), MovementType.DEPOSIT, "SYSTEM_VAULT");
        assertThat(account.balance()).isEqualByComparingTo("1000");

        account.debit("tx2", new BigDecimal("400"), MovementType.TRANSFER, "acc-2");
        assertThat(account.balance()).isEqualByComparingTo("600");
    }

    @Test
    void customer_cannot_be_debited_below_zero() {
        AccountAggregate account = opened("acc-1", AccountType.CUSTOMER);
        account.credit("tx1", new BigDecimal("100"), MovementType.DEPOSIT, "SYSTEM_VAULT");
        int eventsBefore = account.uncommittedEvents().size();

        assertThatThrownBy(() -> account.debit("tx2", new BigDecimal("150"), MovementType.WITHDRAWAL, "SYSTEM_VAULT"))
                .isInstanceOf(InsufficientFundsException.class);

        // Số dư không đổi và không phát thêm event khi bị từ chối.
        assertThat(account.balance()).isEqualByComparingTo("100");
        assertThat(account.uncommittedEvents()).hasSize(eventsBefore);
    }

    @Test
    void system_vault_may_go_negative() {
        AccountAggregate vault = opened("SYSTEM_VAULT", AccountType.SYSTEM_VAULT);

        vault.debit("tx1", new BigDecimal("500"), MovementType.DEPOSIT, "acc-1");

        assertThat(vault.balance()).isEqualByComparingTo("-500");
    }

    @Test
    void amount_must_be_positive() {
        AccountAggregate account = opened("acc-1", AccountType.CUSTOMER);

        assertThatThrownBy(() -> account.credit("tx", BigDecimal.ZERO, MovementType.DEPOSIT, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.debit("tx", new BigDecimal("-5"), MovementType.WITHDRAWAL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replay_reconstructs_balance_from_postings() {
        AccountAggregate original = opened("acc-1", AccountType.CUSTOMER);
        original.credit("tx1", new BigDecimal("1000"), MovementType.DEPOSIT, "SYSTEM_VAULT");
        original.debit("tx2", new BigDecimal("250"), MovementType.WITHDRAWAL, "SYSTEM_VAULT");
        List<DomainEvent> history = original.uncommittedEvents();

        AccountAggregate reconstructed = new AccountAggregate();
        reconstructed.replay(history);

        assertThat(reconstructed.balance()).isEqualByComparingTo("750");
        assertThat(reconstructed.version()).isEqualTo(3); // open + 2 postings
    }

    @Test
    void new_aggregate_has_version_zero() {
        assertThat(new AccountAggregate().version()).isEqualTo(0);
    }

    private static AccountAggregate opened(String id, AccountType type) {
        AccountAggregate account = new AccountAggregate();
        account.open(id, "owner-" + id, type);
        return account;
    }
}
