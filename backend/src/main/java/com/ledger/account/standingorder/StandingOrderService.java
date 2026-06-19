package com.ledger.account.standingorder;

import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.query.AccountQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Quản lý và thực thi lệnh chuyển tiền định kỳ. Mỗi lần đến hạn, gọi lại đúng luồng
 * chuyển tiền (ghi sổ kép, idempotency nội bộ qua txId mới, retry) rồi dời sang chu kỳ sau.
 */
@Service
public class StandingOrderService {

    private static final Logger log = LoggerFactory.getLogger(StandingOrderService.class);

    private final StandingOrderRepository repository;
    private final MoneyMovementHandler money;
    private final AccountQueryService accountQuery;

    public StandingOrderService(
            StandingOrderRepository repository, MoneyMovementHandler money, AccountQueryService accountQuery) {
        this.repository = repository;
        this.money = money;
        this.accountQuery = accountQuery;
    }

    public UUID create(String ownerUserId, String fromAccountId, String toAccountId, BigDecimal amount, long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("Chu kỳ phải lớn hơn 0 giây");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Không thể chuyển cho chính tài khoản đó");
        }
        // Tránh lệnh "chết âm thầm": tài khoản nhận phải tồn tại ngay lúc tạo.
        if (accountQuery.findBalance(toAccountId).isEmpty()) {
            throw new IllegalArgumentException("Tài khoản nhận không tồn tại");
        }
        Instant now = Instant.now();
        StandingOrder order = new StandingOrder(
                UUID.randomUUID(), ownerUserId, fromAccountId, toAccountId, amount, intervalSeconds, now, now);
        repository.save(order);
        return order.getId();
    }

    public List<StandingOrder> listFor(String ownerUserId) {
        return repository.findByOwnerUserId(ownerUserId);
    }

    /** Thực thi mọi lệnh đến hạn; trả về số lệnh chuyển thành công. */
    public int runDue(Instant now) {
        int executed = 0;
        for (StandingOrder order : repository.findByActiveTrueAndNextRunAtLessThanEqual(now)) {
            // Dời chu kỳ và lưu TRƯỚC khi chuyển: chuyển tiền không idempotent (mỗi lần một
            // txId mới), nên ưu tiên "at-most-once" — nếu crash sau khi chuyển, lệnh đã được
            // dời nên không chạy lại (không trừ tiền hai lần). Mất một kỳ khi crash an toàn hơn.
            order.advance();
            repository.save(order);
            try {
                money.transfer(new TransferCommand(order.getFromAccountId(), order.getToAccountId(), order.getAmount()));
                executed++;
            } catch (InsufficientFundsException e) {
                log.info("Standing order {} bỏ qua kỳ này: không đủ số dư", order.getId());
            } catch (RuntimeException e) {
                log.warn("Standing order {} lỗi: {}", order.getId(), e.getMessage());
            }
        }
        return executed;
    }
}
