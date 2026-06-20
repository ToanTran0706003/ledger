package com.ledger.account.interest;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.domain.AccountOpened;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.Direction;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.account.interest.InterestCalculator.BalanceSegment;
import com.ledger.shared.domain.DomainEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Dựng đoạn số dư từ chuỗi event (đầu vào của tính lãi). */
class InterestServiceSegmentsTest {

    @Test
    void builds_time_weighted_segments_from_events() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plus(Duration.ofDays(30)); // nạp 1.000.000 sau 30 ngày
        Instant to = t0.plus(Duration.ofDays(60)); // tính tới ngày 60

        List<DomainEvent> events = List.of(
                new AccountOpened("acc", "owner", AccountType.SAVINGS, "VND", t0),
                new MoneyPosted("acc", "tx1", Direction.CREDIT, new BigDecimal("1000000"),
                        com.ledger.account.domain.MovementType.DEPOSIT, "SYSTEM_VAULT", t1, null));

        List<BalanceSegment> segments = InterestService.buildSegments(events, t0, to);

        // Đoạn 1: số dư 0 trong 30 ngày; đoạn 2: số dư 1.000.000 trong 30 ngày.
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).balance()).isEqualByComparingTo("0");
        assertThat(segments.get(0).duration()).isEqualTo(Duration.ofDays(30));
        assertThat(segments.get(1).balance()).isEqualByComparingTo("1000000");
        assertThat(segments.get(1).duration()).isEqualTo(Duration.ofDays(30));
    }
}
