package com.ledger.account.projection;

import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính năng "wao" của Phase 1: xóa read model rồi dựng lại từ event store.
 * Vì read model là dữ liệu phái sinh, rebuild luôn phải ra kết quả y hệt.
 */
@Service
public class ReadModelRebuildService {

    private final EventStore eventStore;
    private final AccountBalanceProjector projector;
    private final JdbcTemplate jdbc;

    public ReadModelRebuildService(EventStore eventStore, AccountBalanceProjector projector, JdbcTemplate jdbc) {
        this.eventStore = eventStore;
        this.projector = projector;
        this.jdbc = jdbc;
    }

    @Transactional
    public void rebuild() {
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        List<DomainEvent> allEvents = eventStore.loadAll();
        for (DomainEvent event : allEvents) {
            projector.on(event);
        }
    }
}
