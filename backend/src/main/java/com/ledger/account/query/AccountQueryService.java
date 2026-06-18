package com.ledger.account.query;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Đọc thẳng từ read model — không replay, nhanh như CRUD. */
@Service
public class AccountQueryService {

    private final JdbcTemplate jdbc;

    public AccountQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AccountBalanceView> findBalance(String accountId) {
        return jdbc
                .query(
                        """
                        SELECT account_id, owner, currency, balance, available, status
                        FROM rm_account_balance
                        WHERE account_id = ?
                        """,
                        (rs, n) -> new AccountBalanceView(
                                rs.getString("account_id"),
                                rs.getString("owner"),
                                rs.getString("currency"),
                                rs.getBigDecimal("balance"),
                                rs.getBigDecimal("available"),
                                rs.getString("status")),
                        accountId)
                .stream()
                .findFirst();
    }
}
