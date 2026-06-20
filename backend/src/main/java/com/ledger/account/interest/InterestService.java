package com.ledger.account.interest;

import com.ledger.account.command.AccountRepository;
import com.ledger.account.domain.AccountAggregate;
import com.ledger.account.domain.AccountNotFoundException;
import com.ledger.account.domain.AccountOpened;
import com.ledger.account.domain.AccountType;
import com.ledger.account.domain.Direction;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.account.domain.MovementType;
import com.ledger.account.domain.SystemAccounts;
import com.ledger.account.interest.InterestCalculator.BalanceSegment;
import com.ledger.shared.concurrency.RetryingTransactionExecutor;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.eventstore.EventStore;
import com.ledger.shared.outbox.OutboxRelay;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tính và ghi nhận lãi cho tài khoản tiết kiệm bằng cách REPLAY lịch sử số dư: dựng các
 * đoạn (số dư × thời gian) từ chuỗi event rồi áp lãi suất (xem {@link InterestCalculator}).
 * Lãi trả từ SYSTEM_VAULT sang tài khoản (ghi sổ kép) nên tổng tiền hệ thống vẫn cân.
 */
@Service
public class InterestService {

    private final EventStore eventStore;
    private final AccountRepository repository;
    private final RetryingTransactionExecutor executor;
    private final OutboxRelay relay;
    private final BigDecimal annualRate;

    public InterestService(
            EventStore eventStore,
            AccountRepository repository,
            RetryingTransactionExecutor executor,
            OutboxRelay relay,
            @Value("${ledger.savings.annual-rate:0.05}") BigDecimal annualRate) {
        this.eventStore = eventStore;
        this.repository = repository;
        this.executor = executor;
        this.relay = relay;
        this.annualRate = annualRate;
    }

    /** @return số tiền lãi đã ghi nhận (0 nếu không đáng kể). */
    public BigDecimal accrue(String accountId, Instant asOf) {
        // Không tính lãi cho thời gian chưa trôi: chặn asOf không vượt quá hiện tại.
        Instant now = Instant.now();
        Instant to = asOf.isAfter(now) ? now : asOf;

        // Tính lãi VÀ ghi posting trong CÙNG một transaction (executor): nếu hai lần accrue
        // chạy đồng thời, cả hai đụng vault -> một bên dính ConcurrencyConflict -> retry sẽ
        // tính lại "from" (đã thấy posting INTEREST vừa ghi) -> lãi = 0. Nhờ vậy không trả lãi
        // hai lần cho cùng một kỳ. (Đây là lý do không tách phần tính ra ngoài transaction.)
        BigDecimal interest = executor.execute(() -> {
            AccountAggregate account = repository.load(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
            if (account.type() != AccountType.SAVINGS) {
                throw new IllegalArgumentException("Chỉ tài khoản tiết kiệm mới được tính lãi");
            }

            List<DomainEvent> events = eventStore.loadStreamUntil(accountId, to);
            Instant from = lastAccrualOrOpen(events);
            List<BalanceSegment> segments = buildSegments(events, from, to);
            BigDecimal amount = InterestCalculator.interest(segments, annualRate);
            if (amount.signum() <= 0) {
                return BigDecimal.ZERO;
            }

            // Lãi đến từ két hệ thống CÙNG TIỀN TỆ: vault ghi nợ, tiết kiệm ghi có (cân vế theo currency).
            String txId = UUID.randomUUID().toString();
            String vaultId = SystemAccounts.vaultFor(account.currency());
            AccountAggregate vault = repository.load(vaultId).orElseThrow();
            vault.debit(txId, amount, MovementType.INTEREST, accountId);
            account.credit(txId, amount, MovementType.INTEREST, vaultId);
            repository.append(vault);
            repository.append(account);
            return amount;
        });

        relay.drainQuietly();
        return interest;
    }

    /** Mốc bắt đầu tính lãi: lần trả lãi gần nhất, hoặc thời điểm mở tài khoản. */
    private static Instant lastAccrualOrOpen(List<DomainEvent> events) {
        Instant open = null;
        Instant lastInterest = null;
        for (DomainEvent e : events) {
            if (e instanceof AccountOpened o && open == null) {
                open = o.openedAt();
            } else if (e instanceof MoneyPosted m && m.movementType() == MovementType.INTEREST) {
                lastInterest = m.postedAt();
            }
        }
        return lastInterest != null ? lastInterest : open;
    }

    /** Dựng các đoạn (số dư giữ trong bao lâu) trong khoảng [from, to] từ chuỗi event. */
    static List<BalanceSegment> buildSegments(List<DomainEvent> events, Instant from, Instant to) {
        List<Instant> times = new ArrayList<>();
        List<BigDecimal> balances = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (DomainEvent e : events) {
            if (e instanceof AccountOpened o) {
                times.add(o.openedAt());
                balances.add(BigDecimal.ZERO);
            } else if (e instanceof MoneyPosted m) {
                running = m.direction() == Direction.CREDIT ? running.add(m.amount()) : running.subtract(m.amount());
                times.add(m.postedAt());
                balances.add(running);
            }
        }

        List<BalanceSegment> segments = new ArrayList<>();
        if (from == null || !to.isAfter(from)) {
            return segments;
        }
        Instant cursor = from;
        BigDecimal current = balanceAt(times, balances, from);
        for (int i = 0; i < times.size(); i++) {
            Instant t = times.get(i);
            if (t.isAfter(from) && t.isBefore(to)) {
                segments.add(new BalanceSegment(current, Duration.between(cursor, t)));
                cursor = t;
                current = balances.get(i);
            }
        }
        segments.add(new BalanceSegment(current, Duration.between(cursor, to)));
        return segments;
    }

    /** Số dư hiệu lực tại thời điểm t = số dư sau điểm gần nhất có thời gian {@literal <=} t. */
    private static BigDecimal balanceAt(List<Instant> times, List<BigDecimal> balances, Instant t) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < times.size(); i++) {
            if (!times.get(i).isAfter(t)) {
                result = balances.get(i);
            } else {
                break;
            }
        }
        return result;
    }
}
