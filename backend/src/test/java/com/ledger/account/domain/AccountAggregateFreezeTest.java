package com.ledger.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Đóng băng tài khoản (freeze) là kiểm soát rủi ro/gian lận: tài khoản FROZEN không được
 * ghi nợ (chặn tiền chảy ra), nhưng vẫn nhận ghi có. Mở băng (unfreeze) khôi phục ghi nợ.
 */
class AccountAggregateFreezeTest {

    @Test
    void frozen_account_cannot_be_debited() {
        AccountAggregate account = funded("acc-1", "1000");

        account.freeze("nghi ngờ gian lận");

        assertThat(account.status()).isEqualTo(AccountStatus.FROZEN);
        assertThatThrownBy(
                        () -> account.debit("tx", new BigDecimal("100"), MovementType.WITHDRAWAL, SystemAccounts.VAULT_ID))
                .isInstanceOf(AccountFrozenException.class);
    }

    @Test
    void frozen_account_still_accepts_credit() {
        AccountAggregate account = funded("acc-1", "1000");
        account.freeze("khoá tạm");

        account.credit("tx", new BigDecimal("500"), MovementType.DEPOSIT, SystemAccounts.VAULT_ID);

        assertThat(account.balance()).isEqualByComparingTo("1500");
    }

    @Test
    void unfreeze_restores_debit() {
        AccountAggregate account = funded("acc-1", "1000");
        account.freeze("khoá");
        account.unfreeze();

        account.debit("tx", new BigDecimal("100"), MovementType.WITHDRAWAL, SystemAccounts.VAULT_ID);

        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance()).isEqualByComparingTo("900");
    }

    @Test
    void cannot_freeze_an_already_frozen_account() {
        AccountAggregate account = funded("acc-1", "1000");
        account.freeze("lần 1");

        assertThatThrownBy(() -> account.freeze("lần 2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannot_unfreeze_an_active_account() {
        AccountAggregate account = funded("acc-1", "1000");

        assertThatThrownBy(account::unfreeze).isInstanceOf(IllegalStateException.class);
    }

    private static AccountAggregate funded(String id, String amount) {
        AccountAggregate account = new AccountAggregate();
        account.open(id, "owner-" + id, AccountType.CUSTOMER);
        account.credit("seed", new BigDecimal(amount), MovementType.DEPOSIT, SystemAccounts.VAULT_ID);
        return account;
    }
}
