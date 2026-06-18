package com.ledger.account.projection;

import com.ledger.account.domain.AccountOpened;
import com.ledger.shared.domain.DomainEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lắng nghe event của account và cập nhật read model rm_account_balance. */
@Component
public class AccountBalanceProjector {

    private final JdbcTemplate jdbc;

    public AccountBalanceProjector(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void on(DomainEvent event) {
        if (event instanceof AccountOpened e) {
            // ON CONFLICT DO NOTHING -> projector idempotent: chạy lại event cũ không gây lỗi.
            jdbc.update(
                    """
                    INSERT INTO rm_account_balance (account_id, owner, balance, available, status)
                    VALUES (?, ?, 0, 0, 'ACTIVE')
                    ON CONFLICT (account_id) DO NOTHING
                    """,
                    e.accountId(), e.owner());
        }
    }
}
