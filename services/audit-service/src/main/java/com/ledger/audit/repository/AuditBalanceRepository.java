package com.ledger.audit.repository;

import com.ledger.audit.web.CurrencyTotal;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuditBalanceRepository {

    private static final String UNKNOWN_CURRENCY = "UNKNOWN";

    private final JdbcTemplate jdbcTemplate;

    public AuditBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void accountOpened(String accountId, String currency) {
        jdbcTemplate.update("""
                INSERT INTO audit_account_balance (account_id, currency, balance)
                VALUES (?, ?, 0)
                ON CONFLICT (account_id) DO UPDATE
                SET currency = EXCLUDED.currency
                WHERE audit_account_balance.currency = ?
                """, accountId, currency, UNKNOWN_CURRENCY);
    }

    @Transactional
    public void moneyPosted(String accountId, String txId, BigDecimal signedAmount) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO audit_processed_money_posted (account_id, tx_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, accountId, txId);
        if (inserted == 0) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO audit_account_balance (account_id, currency, balance)
                VALUES (?, ?, ?)
                ON CONFLICT (account_id) DO UPDATE
                SET balance = audit_account_balance.balance + EXCLUDED.balance
                """, accountId, UNKNOWN_CURRENCY, signedAmount);
    }

    public List<CurrencyTotal> perCurrencyTotals() {
        return jdbcTemplate.query("""
                SELECT currency, COALESCE(SUM(balance), 0) AS total
                FROM audit_account_balance
                GROUP BY currency
                ORDER BY currency
                """, (rs, rowNum) -> new CurrencyTotal(
                rs.getString("currency"),
                rs.getBigDecimal("total")
        ));
    }

    public long countAccounts() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_account_balance",
                Long.class
        );
        return count == null ? 0 : count;
    }
}
