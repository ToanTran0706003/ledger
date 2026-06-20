package com.ledger.account.command;

import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.config.LedgerProperties;
import com.ledger.shared.outbox.OutboxRelay;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Khai sinh một SYSTEM_VAULT cho MỖI tiền tệ với số dư khởi tạo (bút toán GENESIS). Đây là vế
 * tiền "được phát hành" duy nhất không có đối ứng; sau đó tổng số dư theo TỪNG tiền tệ luôn bằng
 * seedAmount (cơ sở cho integrity per-currency). Vault VND giữ id cũ "SYSTEM_VAULT" (tương thích
 * ngược). Idempotent kể cả khi chạy đồng thời: hai tiến trình cùng seed thì một bên dính
 * ConcurrencyConflict rồi thấy vault đã có.
 */
@Service
public class VaultSeedService {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final LedgerProperties properties;
    private final List<String> currencies;

    public VaultSeedService(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            LedgerProperties properties,
            @Value("${ledger.vault.currencies:VND}") List<String> currencies) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.properties = properties;
        this.currencies = currencies;
    }

    public void seedIfAbsent() {
        for (String currency : currencies) {
            seedVault(currency.trim());
        }
    }

    private void seedVault(String currency) {
        String vaultId = SystemAccounts.vaultFor(currency);
        boolean seeded = executor.execute(() -> {
            if (repository.load(vaultId).isPresent()) {
                return false;
            }
            AccountAggregate vault = new AccountAggregate();
            vault.open(vaultId, "System Vault " + currency, AccountType.SYSTEM_VAULT, currency);
            vault.credit(UUID.randomUUID().toString(), properties.vault().seedAmount(), MovementType.GENESIS, null);
            repository.append(vault);
            return true;
        });

        if (seeded) {
            relay.drainQuietly();
        }
    }
}
