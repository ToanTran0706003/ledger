package com.ledger.account.command;

import com.ledger.account.domain.DailyLimitExceededException;
import java.math.BigDecimal;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hạn mức ghi nợ theo ngày cho một tài khoản (rút + chuyển). Kiểm tra ngay TRONG transaction
 * di chuyển tiền, tính tổng ghi nợ hôm nay từ event store (nguồn sự thật, đã commit). Vì các
 * lệnh ghi nợ cùng một tài khoản bị optimistic concurrency serialize (UNIQUE aggregate+version,
 * ADR-0006), tổng đếm được luôn nhất quán — hạn mức chính xác cả khi nhiều request đồng thời.
 *
 * Tắt được qua {@code ledger.daily-limit.enabled} (tắt trong test để không nhiễu kịch bản khác).
 */
@Component
public class DailyLimitGuard {

    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final BigDecimal maxPerDay;
    private final String zone;

    public DailyLimitGuard(
            JdbcTemplate jdbc,
            @Value("${ledger.daily-limit.enabled:true}") boolean enabled,
            @Value("${ledger.daily-limit.max-per-day:500000000}") BigDecimal maxPerDay,
            @Value("${ledger.daily-limit.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.maxPerDay = maxPerDay;
        this.zone = ZoneId.of(zone).getId(); // fail-fast nếu zone cấu hình sai
    }

    /** Ném {@link DailyLimitExceededException} nếu ghi nợ thêm {@code amount} vượt hạn mức ngày. */
    public void check(String accountId, BigDecimal amount) {
        if (!enabled) {
            return;
        }
        // Ranh giới đầu ngày tính BẰNG ĐỒNG HỒ DB (cùng nguồn với occurred_at) -> không lệch
        // do drift JVM/DB. now() AT TIME ZONE zone -> giờ địa phương; truncate ngày; đổi lại timestamptz.
        BigDecimal today = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM((payload->>'amount')::numeric), 0)
                FROM events
                WHERE aggregate_id = ?
                  AND event_type = 'MoneyPosted'
                  AND payload->>'direction' = 'DEBIT'
                  AND payload->>'movementType' IN ('WITHDRAWAL', 'TRANSFER')
                  AND occurred_at >= date_trunc('day', now() AT TIME ZONE ?) AT TIME ZONE ?
                """,
                BigDecimal.class, accountId, zone, zone);
        if (today.add(amount).compareTo(maxPerDay) > 0) {
            throw new DailyLimitExceededException(accountId, maxPerDay, today, amount);
        }
    }
}
