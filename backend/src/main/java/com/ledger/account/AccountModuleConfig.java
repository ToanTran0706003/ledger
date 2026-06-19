package com.ledger.account;

import com.ledger.account.domain.AccountFrozen;
import com.ledger.account.domain.AccountOpened;
import com.ledger.account.domain.AccountUnfrozen;
import com.ledger.account.domain.HoldPlaced;
import com.ledger.account.domain.HoldReleased;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.shared.eventstore.EventTypeRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký các event của module account vào registry chung. Giữ ranh giới module:
 * kernel shared không cần biết về event cụ thể của account (ADR-0001).
 */
@Configuration
public class AccountModuleConfig {

    public AccountModuleConfig(EventTypeRegistry registry) {
        registry.register(AccountOpened.class);
        registry.register(MoneyPosted.class);
        registry.register(HoldPlaced.class);
        registry.register(HoldReleased.class);
        registry.register(AccountFrozen.class);
        registry.register(AccountUnfrozen.class);
    }
}
