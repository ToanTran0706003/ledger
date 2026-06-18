package com.ledger.audit.query;

import java.math.BigDecimal;

/**
 * Kết quả kiểm tra toàn vẹn sổ cái. balanced=false nghĩa là tổng số dư lệch khỏi
 * lượng tiền đã phát hành -> có bug nghiêm trọng (tiền tự sinh/bốc hơi).
 */
public record IntegrityReport(
        BigDecimal totalBalance,
        BigDecimal expectedTotal,
        boolean balanced) {
}
