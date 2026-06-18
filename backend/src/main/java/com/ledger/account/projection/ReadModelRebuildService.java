package com.ledger.account.projection;

import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.projection.ProjectionDispatcher;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính năng "wao": xóa read model rồi dựng lại từ event store theo đúng thứ tự
 * global_seq. Vì read model là dữ liệu phái sinh, rebuild luôn ra kết quả y hệt.
 */
@Service
public class ReadModelRebuildService {

    private final EventStore eventStore;
    private final ProjectionDispatcher dispatcher;
    private final JdbcTemplate jdbc;

    public ReadModelRebuildService(EventStore eventStore, ProjectionDispatcher dispatcher, JdbcTemplate jdbc) {
        this.eventStore = eventStore;
        this.dispatcher = dispatcher;
        this.jdbc = jdbc;
    }

    @Transactional
    public void rebuild() {
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");

        List<DomainEvent> allEvents = eventStore.loadAll();
        for (DomainEvent event : allEvents) {
            dispatcher.dispatch(event);
        }
    }
}
