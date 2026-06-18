package com.ledger.audit.query;

import com.ledger.shared.config.LedgerProperties;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Invariant toàn cục của ghi sổ kép: tổng số dư mọi tài khoản (kể cả SYSTEM_VAULT)
 * luôn bằng lượng tiền khai sinh (seedAmount). Mỗi giao dịch cân vế nên tổng không đổi.
 */
@Service
public class IntegrityService {

    private final JdbcTemplate jdbc;
    private final LedgerProperties properties;

    public IntegrityService(JdbcTemplate jdbc, LedgerProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public IntegrityReport check() {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(balance), 0) FROM rm_account_balance", BigDecimal.class);
        BigDecimal expected = properties.vault().seedAmount();
        boolean balanced = total.compareTo(expected) == 0;
        return new IntegrityReport(total, expected, balanced);
    }
}
