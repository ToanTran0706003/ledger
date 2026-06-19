package com.ledger.account.query;

import java.util.List;
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

    public List<AccountBalanceView> findByOwner(String owner) {
        return jdbc.query(
                """
                SELECT account_id, owner, account_type, currency, balance, available, status
                FROM rm_account_balance
                WHERE owner = ?
                ORDER BY account_id
                """,
                (rs, n) -> mapBalance(rs),
                owner);
    }

    public List<TransactionHistoryView> findHistory(String accountId) {
        return jdbc.query(
                """
                SELECT tx_id, direction, amount, counterparty, balance_after, movement_type, occurred_at
                FROM rm_transaction_history
                WHERE account_id = ?
                ORDER BY occurred_at DESC, id DESC
                """,
                (rs, n) -> new TransactionHistoryView(
                        rs.getString("tx_id"),
                        rs.getString("direction"),
                        rs.getBigDecimal("amount"),
                        rs.getString("counterparty"),
                        rs.getBigDecimal("balance_after"),
                        rs.getString("movement_type"),
                        rs.getTimestamp("occurred_at").toInstant()),
                accountId);
    }

    public Optional<AccountBalanceView> findBalance(String accountId) {
        return jdbc
                .query(
                        """
                        SELECT account_id, owner, account_type, currency, balance, available, status
                        FROM rm_account_balance
                        WHERE account_id = ?
                        """,
                        (rs, n) -> mapBalance(rs),
                        accountId)
                .stream()
                .findFirst();
    }

    private static AccountBalanceView mapBalance(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AccountBalanceView(
                rs.getString("account_id"),
                rs.getString("owner"),
                rs.getString("account_type"),
                rs.getString("currency"),
                rs.getBigDecimal("balance"),
                rs.getBigDecimal("available"),
                rs.getString("status"));
    }
}
