package com.ledger.account.query;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Time-travel: số dư của một tài khoản tại một thời điểm trong quá khứ, dựng lại bằng
 * cách replay các event xảy ra tại/trước thời điểm đó. Đây là sức mạnh độc nhất của
 * Event Sourcing — trả lời được "số dư ngày X".
 */
@Service
public class TimeTravelQueryService {

    private final EventStore eventStore;

    public TimeTravelQueryService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Optional<BigDecimal> balanceAsOf(String accountId, Instant asOf) {
        List<DomainEvent> events = eventStore.loadStreamUntil(accountId, asOf);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        AccountAggregate aggregate = new AccountAggregate();
        aggregate.replay(events);
        return Optional.of(aggregate.balance());
    }
}
