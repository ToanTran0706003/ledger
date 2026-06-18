package com.ledger.shared.concurrency;

import com.ledger.shared.eventstore.ConcurrencyConflictException;
import com.ledger.shared.observability.LedgerMetrics;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chạy một thao tác ghi trong transaction; nếu gặp ConcurrencyConflictException
 * (hai request cùng ghi một version) thì thử lại với transaction mới. Mỗi lần thử
 * sẽ load lại aggregate ở version mới và validate lại invariant (xem 04-security mục 4).
 */
@Component
public class RetryingTransactionExecutor {

    private static final int DEFAULT_MAX_ATTEMPTS = 20;

    private final TransactionTemplate transactionTemplate;
    private final LedgerMetrics metrics;

    public RetryingTransactionExecutor(TransactionTemplate transactionTemplate, LedgerMetrics metrics) {
        this.transactionTemplate = transactionTemplate;
        this.metrics = metrics;
    }

    public <T> T execute(Supplier<T> action) {
        return execute(DEFAULT_MAX_ATTEMPTS, action);
    }

    public <T> T execute(int maxAttempts, Supplier<T> action) {
        ConcurrencyConflictException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return transactionTemplate.execute(status -> action.get());
            } catch (ConcurrencyConflictException e) {
                last = e;
                metrics.recordConflict();
                backoff(attempt);
            }
        }
        throw last;
    }

    private static void backoff(int attempt) {
        try {
            // Backoff tuyến tính + jitter để giảm khả năng các thread đụng nhau lặp lại.
            long millis = Math.min(50L, attempt * 2L) + ThreadLocalRandom.current().nextLong(5);
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi retry", ie);
        }
    }
}
