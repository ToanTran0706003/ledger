package com.ledger.account.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Tính lãi đơn theo số dư bình quân gia quyền thời gian (time-weighted): với mỗi đoạn
 * số dư giữ trong một khoảng thời gian, lãi = số dư × thời gian × lãi suất năm / (1 năm).
 * Hàm thuần, không phụ thuộc hạ tầng — dễ kiểm thử (viết test trước, TDD).
 */
public final class InterestCalculator {

    private static final BigDecimal SECONDS_PER_YEAR = BigDecimal.valueOf(365L * 24 * 3600);

    public record BalanceSegment(BigDecimal balance, Duration duration) {}

    private InterestCalculator() {}

    public static BigDecimal interest(List<BalanceSegment> segments, BigDecimal annualRate) {
        BigDecimal weighted = BigDecimal.ZERO; // Σ (số dư × số giây × lãi suất)
        for (BalanceSegment seg : segments) {
            // Số giây CÓ phần lẻ (nano) — không dùng getSeconds() vì nó cắt mất dưới giây.
            BigDecimal seconds = BigDecimal.valueOf(seg.duration().getSeconds())
                    .add(BigDecimal.valueOf(seg.duration().getNano(), 9));
            weighted = weighted.add(seg.balance().multiply(seconds).multiply(annualRate));
        }
        // Chia cho số giây một năm, làm tròn xuống tới 2 chữ số (không trả lãi nhiều hơn thực tế).
        return weighted.divide(SECONDS_PER_YEAR, 2, RoundingMode.FLOOR);
    }
}
