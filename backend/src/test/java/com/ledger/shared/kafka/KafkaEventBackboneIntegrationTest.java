package com.ledger.shared.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.VaultSeedService;
import com.ledger.account.domain.AccountType;
import com.ledger.account.query.AccountQueryService;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Kafka event backbone (ADR-0023): khi bật, outbox PUBLISH event lên Kafka và projection chạy bất
 * đồng bộ ở consumer. Dùng broker Kafka nhúng (in-JVM, không cần Docker) để kiểm end-to-end:
 * nạp tiền -> event qua topic -> read model cập nhật (eventually).
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "ledger.events")
@TestPropertySource(
        properties = {
            "ledger.kafka.enabled=true",
            "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
            "ledger.outbox.scheduler-enabled=true",
            "ledger.outbox.poll-interval-ms=300"
        })
class KafkaEventBackboneIntegrationTest {

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private MoneyMovementHandler money;

    @Autowired
    private AccountQueryService accountQuery;

    @Autowired
    private VaultSeedService vaultSeed;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        jdbc.update("TRUNCATE TABLE rm_hold");
        vaultSeed.seedIfAbsent();
    }

    @Test
    void deposit_event_flows_through_kafka_to_read_model() {
        String acc = openAccount.handle(new OpenAccountCommand("Kafka", AccountType.CUSTOMER));
        money.deposit(new DepositCommand(acc, new BigDecimal("1000")));

        // Projection chạy ASYNC qua Kafka (outbox -> topic -> consumer) -> chờ read model phản ánh.
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> assertThat(accountQuery.findBalance(acc))
                        .isPresent()
                        .get()
                        .satisfies(v -> assertThat(v.balance()).isEqualByComparingTo("1000")));
    }
}
