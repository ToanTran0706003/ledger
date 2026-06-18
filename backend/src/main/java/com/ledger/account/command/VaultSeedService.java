package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.config.LedgerProperties;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.projection.ProjectionDispatcher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khai sinh SYSTEM_VAULT với số dư khởi tạo (bút toán GENESIS). Đây là vế tiền
 * "được phát hành" duy nhất không có đối ứng; tổng tiền toàn hệ thống sau đó luôn
 * bằng seedAmount (cơ sở cho integrity check).
 */
@Service
public class VaultSeedService {

    private static final String AGGREGATE_TYPE = "Account";

    private final EventStore eventStore;
    private final AccountRepository repository;
    private final ProjectionDispatcher dispatcher;
    private final LedgerProperties properties;

    public VaultSeedService(
            EventStore eventStore,
            AccountRepository repository,
            ProjectionDispatcher dispatcher,
            LedgerProperties properties) {
        this.eventStore = eventStore;
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Transactional
    public void seedIfAbsent() {
        if (repository.load(SystemAccounts.VAULT_ID).isPresent()) {
            return;
        }

        AccountAggregate vault = new AccountAggregate();
        vault.open(SystemAccounts.VAULT_ID, "System Vault", AccountType.SYSTEM_VAULT);
        vault.credit(UUID.randomUUID().toString(), properties.vault().seedAmount(), MovementType.GENESIS, null);

        eventStore.append(SystemAccounts.VAULT_ID, AGGREGATE_TYPE, vault.version(), vault.uncommittedEvents());
        for (DomainEvent event : vault.uncommittedEvents()) {
            dispatcher.dispatch(event);
        }
        vault.markEventsCommitted();
    }
}
