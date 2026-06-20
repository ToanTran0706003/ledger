package com.ledger.account.fx;

import com.ledger.account.command.AccountRepository;
import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Quy đổi tiền tệ (FX) giữa hai tài khoản khác tiền tệ của cùng người dùng. Thực hiện bằng HAI bút
 * toán ghi sổ kép CÙNG TIỀN TỆ, bắc cầu qua hai vault và một tỉ giá cấu hình:
 *   nguồn(ccyA) → vault(A)   và   vault(B) → đích(ccyB) ở amount × rate.
 * Nhờ vậy tổng theo TỪNG tiền tệ không đổi (integrity per-currency vẫn cân); hệ thống đóng vai
 * "quầy FX" hấp thụ tiền tệ A và cấp tiền tệ B (vault được phép âm).
 */
@Service
public class ExchangeHandler {

    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final FxRateService fxRates;

    public ExchangeHandler(
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            FxRateService fxRates) {
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.fxRates = fxRates;
    }

    /** Quy đổi {@code amount} (tiền tệ của from) sang tiền tệ của to theo tỉ giá. Trả về txId. */
    public String exchange(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Không thể quy đổi trên cùng một tài khoản");
        }
        String txId = UUID.randomUUID().toString();
        String result = executor.execute(() -> {
            AccountAggregate from = load(fromAccountId);
            AccountAggregate to = load(toAccountId);
            if (from.currency().equals(to.currency())) {
                throw new IllegalArgumentException("Hai tài khoản cùng tiền tệ — dùng chuyển tiền thay vì FX");
            }

            // Làm tròn về 2 chữ số = đúng scale của read model NUMERIC(20,2). Nếu không, vế FX lưu
            // chính xác trong event store nhưng read model làm tròn lệch -> integrity báo lệch sổ giả.
            BigDecimal toAmount = amount.multiply(fxRates.rate(from.currency(), to.currency()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (toAmount.signum() <= 0) {
                throw new IllegalArgumentException("Số tiền quy đổi quá nhỏ");
            }
            String fromVaultId = SystemAccounts.vaultFor(from.currency());
            String toVaultId = SystemAccounts.vaultFor(to.currency());
            AccountAggregate fromVault = load(fromVaultId);
            AccountAggregate toVault = load(toVaultId);

            // Vế tiền tệ nguồn: from -> vault(A)
            from.debit(txId, amount, MovementType.EXCHANGE, fromVaultId);
            fromVault.credit(txId, amount, MovementType.EXCHANGE, fromAccountId);
            // Vế tiền tệ đích: vault(B) -> to
            toVault.debit(txId, toAmount, MovementType.EXCHANGE, toAccountId);
            to.credit(txId, toAmount, MovementType.EXCHANGE, toVaultId);

            repository.append(from);
            repository.append(fromVault);
            repository.append(toVault);
            repository.append(to);
            return txId;
        });
        relay.drainQuietly();
        return result;
    }

    private AccountAggregate load(String accountId) {
        return repository.load(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
