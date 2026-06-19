package com.ledger.account.fraud;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Đọc các tài khoản đang bị đóng băng từ read model — cho màn giám sát gian lận của ADMIN. */
@Service
public class FraudQueryService {

    private final JdbcTemplate jdbc;

    public FraudQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FrozenAccountView> listFrozen() {
        return jdbc.query(
                """
                SELECT account_id, owner, freeze_reason
                FROM rm_account_balance
                WHERE status = 'FROZEN'
                ORDER BY updated_at DESC
                """,
                (rs, n) -> new FrozenAccountView(
                        rs.getString("account_id"), rs.getString("owner"), rs.getString("freeze_reason")));
    }
}
