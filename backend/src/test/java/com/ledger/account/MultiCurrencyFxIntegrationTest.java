package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.fx.ExchangeHandler;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Đa tiền tệ + quy đổi (FX). Mỗi tài khoản có một tiền tệ; mỗi tiền tệ có một vault riêng được
 * khai sinh seedAmount. Chuyển tiền cùng tiền tệ; khác tiền tệ phải qua FX. FX là HAI bút toán
 * ghi sổ kép cùng tiền tệ (nguồn→vault nguồn, vault đích→đích) ở tỉ giá cấu hình, nên **mỗi tiền
 * tệ vẫn cân độc lập** (tổng theo từng currency == seedAmount).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "ledger.vault.currencies=VND,USD",
            "ledger.fx.rates.VND.USD=0.00004", // 25000 VND = 1 USD
            "ledger.fx.rates.USD.VND=25000"
        })
class MultiCurrencyFxIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private ExchangeHandler exchange;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private IntegrityService integrity;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        jdbc.update("TRUNCATE TABLE rm_hold");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void account_opens_in_its_currency() {
        String usd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "USD"));
        assertThat(accountQuery.findBalance(usd).orElseThrow().currency()).isEqualTo("USD");
        // Mặc định vẫn là VND khi không truyền tiền tệ.
        String vnd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER));
        assertThat(accountQuery.findBalance(vnd).orElseThrow().currency()).isEqualTo("VND");
    }

    @Test
    void transfer_across_currencies_is_rejected() {
        String vnd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "VND"));
        String usd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "USD"));
        money.deposit(new DepositCommand(vnd, new BigDecimal("100000")));

        assertThatThrownBy(() -> money.transfer(new TransferCommand(vnd, usd, new BigDecimal("1000"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fx_exchange_converts_at_rate_and_keeps_each_currency_balanced() {
        String vnd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "VND"));
        String usd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "USD"));
        money.deposit(new DepositCommand(vnd, new BigDecimal("100000")));

        exchange.exchange(vnd, usd, new BigDecimal("25000")); // 25000 VND -> 1 USD

        assertThat(accountQuery.findBalance(vnd).orElseThrow().balance()).isEqualByComparingTo("75000");
        assertThat(accountQuery.findBalance(usd).orElseThrow().balance()).isEqualByComparingTo("1");
        // Mỗi tiền tệ vẫn cân độc lập (tiền không tự sinh/mất trong từng currency).
        assertThat(integrity.check().balanced()).isTrue();
    }

    @Test
    void fx_with_fractional_result_rounds_to_minor_unit_and_stays_balanced() {
        String vnd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "VND"));
        String usd = openAccount.handle(new OpenAccountCommand("u", AccountType.CUSTOMER, "USD"));
        money.deposit(new DepositCommand(vnd, new BigDecimal("100000")));

        exchange.exchange(vnd, usd, new BigDecimal("12345")); // 12345 * 0.00004 = 0.4938 -> 0.49

        assertThat(accountQuery.findBalance(usd).orElseThrow().balance()).isEqualByComparingTo("0.49");
        // Số lẻ làm tròn 2 chữ số khớp read model -> KHÔNG báo lệch sổ giả.
        assertThat(integrity.check().balanced()).isTrue();
    }
}
