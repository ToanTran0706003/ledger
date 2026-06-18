package com.ledger.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.shared.domain.DomainEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountAggregateTest {

    @Test
    void open_raises_AccountOpened_event() {
        // given: một aggregate mới
        AccountAggregate account = new AccountAggregate();

        // when: mở tài khoản
        account.open("acc-1", "Alice", AccountType.CUSTOMER);

        // then: sinh đúng một event AccountOpened với dữ liệu đúng
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
    void open_sets_state_to_active() {
        AccountAggregate account = new AccountAggregate();

        account.open("acc-1", "Alice", AccountType.CUSTOMER);

        assertThat(account.accountId()).isEqualTo("acc-1");
        assertThat(account.owner()).isEqualTo("Alice");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void replay_reconstructs_same_state_as_original() {
        // given: lịch sử event của một tài khoản đã mở
        AccountAggregate original = new AccountAggregate();
        original.open("acc-1", "Alice", AccountType.CUSTOMER);
        List<DomainEvent> history = original.uncommittedEvents();

        // when: dựng lại aggregate bằng replay (mô phỏng load từ event store)
        AccountAggregate reconstructed = new AccountAggregate();
        reconstructed.replay(history);

        // then: trạng thái khớp, và version = số event đã replay
        assertThat(reconstructed.accountId()).isEqualTo("acc-1");
        assertThat(reconstructed.owner()).isEqualTo("Alice");
        assertThat(reconstructed.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(reconstructed.version()).isEqualTo(1);
    }

    @Test
    void new_aggregate_has_version_zero() {
        AccountAggregate account = new AccountAggregate();

        assertThat(account.version()).isEqualTo(0);
    }
}
