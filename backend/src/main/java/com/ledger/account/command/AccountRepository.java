package com.ledger.account.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountSnapshot;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.snapshot.SnapshotStore;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Store của AccountAggregate: load = snapshot (nếu có) + replay event sau đó; append =
 * ghi event mới rồi, mỗi khi vượt mốc N event, ghi snapshot mới (ADR-0008). Tập trung
 * logic snapshot ở một chỗ để command handler không phải bận tâm.
 */
@Service
public class AccountRepository {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    private final ObjectMapper mapper;
    private final int snapshotEveryNEvents;

    public AccountRepository(
            EventStore eventStore,
            SnapshotStore snapshotStore,
            ObjectMapper mapper,
            @Value("${ledger.snapshot.every-n-events:50}") int snapshotEveryNEvents) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.mapper = mapper;
        this.snapshotEveryNEvents = snapshotEveryNEvents;
    }

    public Optional<AccountAggregate> load(String accountId) {
        Optional<SnapshotStore.Snapshot> snapshot = snapshotStore.load(accountId);

        AccountAggregate aggregate = new AccountAggregate();
        int fromVersion = 0;
        if (snapshot.isPresent()) {
            aggregate.restoreFromSnapshot(deserialize(snapshot.get().stateJson()), snapshot.get().version());
            fromVersion = snapshot.get().version();
        }

        List<DomainEvent> events = eventStore.loadStreamAfter(accountId, fromVersion);
        if (fromVersion == 0 && events.isEmpty()) {
            return Optional.empty();
        }
        aggregate.replay(events);
        return Optional.of(aggregate);
    }

    /** Ghi các event chưa commit của aggregate; ghi snapshot khi vượt mốc N event. */
    public void append(AccountAggregate aggregate) {
        List<DomainEvent> newEvents = aggregate.uncommittedEvents();
        if (newEvents.isEmpty()) {
            return;
        }

        int before = aggregate.version();
        eventStore.append(aggregate.accountId(), AGGREGATE_TYPE, before, newEvents);
        int after = before + newEvents.size();

        if (before / snapshotEveryNEvents != after / snapshotEveryNEvents) {
            snapshotStore.save(aggregate.accountId(), AGGREGATE_TYPE, after, serialize(aggregate.toSnapshot()));
        }

        aggregate.markEventsCommitted();
    }

    private String serialize(AccountSnapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được snapshot", e);
        }
    }

    private AccountSnapshot deserialize(String json) {
        try {
            return mapper.readValue(json, AccountSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("Không deserialize được snapshot", e);
        }
    }
}
