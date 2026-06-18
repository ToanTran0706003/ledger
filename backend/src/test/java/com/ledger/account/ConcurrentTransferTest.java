package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.query.AccountQueryService;
import com.ledger.audit.query.IntegrityService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Chống race & double-spend thật: nhiều thread cùng rút tiền (qua transfer) khỏi MỘT
 * tài khoản nguồn, tổng nhu cầu vượt số dư. Optimistic concurrency + retry phải đảm bảo:
 * không bao giờ số dư âm, đúng số giao dịch thành công, tổng tiền không đổi.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentTransferTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private IntegrityService integrity;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void concurrent_withdrawals_never_overdraw() throws InterruptedException {
        int threads = 15;
        BigDecimal amountEach = new BigDecimal("100");
        // Số dư đủ cho đúng 10 giao dịch; 5 giao dịch còn lại phải bị từ chối.
        String source = openAccount.handle(new OpenAccountCommand("Source", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(source, new BigDecimal("1000")));

        List<String> dests = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            dests.add(openAccount.handle(new OpenAccountCommand("Dest-" + i, AccountType.CUSTOMER)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            String dest = dests.get(i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    money.transfer(new TransferCommand(source, dest, amountEach));
                    succeeded.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        go.countDown(); // bắn đồng loạt
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(5);
        assertThat(accountQuery.findBalance(source).orElseThrow().balance()).isEqualByComparingTo("0");
        BigDecimal destTotal = dests.stream()
                .map(d -> accountQuery.findBalance(d).orElseThrow().balance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(destTotal).isEqualByComparingTo("1000");
        assertThat(integrity.check().balanced()).isTrue();
    }
}
