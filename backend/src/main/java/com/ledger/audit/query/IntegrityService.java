package com.ledger.audit.query;

import com.ledger.shared.config.LedgerProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Invariant ghi sổ kép, kiểm tra theo TỪNG tiền tệ: với mỗi currency, tổng số dư mọi tài khoản
 * (kể cả vault của tiền tệ đó) luôn bằng lượng khai sinh (seedAmount). FX là hai bút toán cùng
 * tiền tệ nên không phá invariant này. balanced=true chỉ khi MỌI tiền tệ đều cân.
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
        BigDecimal seed = properties.vault().seedAmount();
        List<Map<String, Object>> perCurrency = jdbc.queryForList(
                "SELECT currency, COALESCE(SUM(balance), 0) AS total FROM rm_account_balance GROUP BY currency");

        boolean balanced = true;
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Map<String, Object> row : perCurrency) {
            BigDecimal total = (BigDecimal) row.get("total");
            totalBalance = totalBalance.add(total);
            if (total.compareTo(seed) != 0) {
                balanced = false;
            }
        }
        // expectedTotal = seedAmount × số tiền tệ (mỗi tiền tệ một vault khai sinh seedAmount).
        BigDecimal expectedTotal = seed.multiply(BigDecimal.valueOf(perCurrency.size()));
        return new IntegrityReport(totalBalance, expectedTotal, balanced);
    }
}
