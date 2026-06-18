package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.config.LedgerProperties;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khai sinh SYSTEM_VAULT với số dư khởi tạo (bút toán GENESIS). Đây là vế tiền
 * "được phát hành" duy nhất không có đối ứng; tổng tiền toàn hệ thống sau đó luôn
 * bằng seedAmount (cơ sở cho integrity check). Idempotent kể cả khi chạy đồng thời:
 * nếu hai tiến trình cùng seed, một bên dính ConcurrencyConflict rồi thấy vault đã có.
 */
@Service
public class VaultSeedService {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final LedgerProperties properties;

    public VaultSeedService(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            LedgerProperties properties) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.properties = properties;
    }

    public void seedIfAbsent() {
        boolean seeded = executor.execute(() -> {
            if (repository.load(SystemAccounts.VAULT_ID).isPresent()) {
                return false;
            }
            AccountAggregate vault = new AccountAggregate();
            vault.open(SystemAccounts.VAULT_ID, "System Vault", AccountType.SYSTEM_VAULT);
            vault.credit(UUID.randomUUID().toString(), properties.vault().seedAmount(), MovementType.GENESIS, null);
            repository.append(vault);
            return true;
        });

        if (seeded) {
            relay.drainQuietly();
        }
    }
}
