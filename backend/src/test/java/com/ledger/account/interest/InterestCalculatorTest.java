package com.ledger.account.interest;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.interest.InterestCalculator.BalanceSegment;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterestCalculatorTest {

    private static final BigDecimal RATE = new BigDecimal("0.05"); // 5%/năm
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    @Test
    void full_year_at_constant_balance() {
        var interest = InterestCalculator.interest(
                List.of(new BalanceSegment(new BigDecimal("1000000"), ONE_YEAR)), RATE);
        assertThat(interest).isEqualByComparingTo("50000.00");
    }

    @Test
    void half_year_is_half_the_interest() {
        var interest = InterestCalculator.interest(
                List.of(new BalanceSegment(new BigDecimal("1000000"), ONE_YEAR.dividedBy(2))), RATE);
        assertThat(interest).isEqualByComparingTo("25000.00");
    }

    @Test
    void sums_over_segments_weighted_by_time() {
        // 1.000.000 trong nửa năm + 2.000.000 trong nửa năm = 25.000 + 50.000.
        var interest = InterestCalculator.interest(
                List.of(
                        new BalanceSegment(new BigDecimal("1000000"), ONE_YEAR.dividedBy(2)),
                        new BalanceSegment(new BigDecimal("2000000"), ONE_YEAR.dividedBy(2))),
                RATE);
        assertThat(interest).isEqualByComparingTo("75000.00");
    }

    @Test
    void zero_balance_yields_no_interest() {
        var interest = InterestCalculator.interest(
                List.of(new BalanceSegment(BigDecimal.ZERO, ONE_YEAR)), RATE);
        assertThat(interest).isEqualByComparingTo("0.00");
    }

    @Test
    void rounds_down_to_two_decimals() {
        // 100 * (1 ngày/365) * 0.05 = 0.0137 -> làm tròn xuống 0.01.
        var interest = InterestCalculator.interest(
                List.of(new BalanceSegment(new BigDecimal("100"), Duration.ofDays(1))), RATE);
        assertThat(interest).isEqualByComparingTo("0.01");
    }
}
