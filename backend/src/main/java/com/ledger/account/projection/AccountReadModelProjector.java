package com.ledger.account.projection;

import com.ledger.account.domain.AccountOpened;
import com.ledger.account.domain.Direction;
import com.ledger.account.domain.HoldPlaced;
import com.ledger.account.domain.HoldReleaseReason;
import com.ledger.account.domain.HoldReleased;
import com.ledger.account.domain.MoneyPosted;
import com.ledger.shared.domain.DomainEvent;
import com.ledger.shared.projection.Projector;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dựng read model của module account từ event: rm_account_balance (số dư hiện tại)
 * và rm_transaction_history (sao kê). Gộp hai read model vào một projector để
 * balance_after của lịch sử luôn nhất quán với số dư sau khi cập nhật.
 */
@Component
public class AccountReadModelProjector implements Projector {

    private final JdbcTemplate jdbc;

    public AccountReadModelProjector(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void on(DomainEvent event) {
        switch (event) {
            case AccountOpened e -> onAccountOpened(e);
            case MoneyPosted e -> onMoneyPosted(e);
            case HoldPlaced e -> onHoldPlaced(e);
            case HoldReleased e -> onHoldReleased(e);
            default -> {
                // event không liên quan tới read model của account -> bỏ qua
            }
        }
    }

    private void onAccountOpened(AccountOpened e) {
        jdbc.update(
                """
                INSERT INTO rm_account_balance (account_id, owner, account_type, balance, available, status)
                VALUES (?, ?, ?, 0, 0, 'ACTIVE')
                ON CONFLICT (account_id) DO NOTHING
                """,
                e.accountId(), e.owner(), e.type().name());
    }

    private void onMoneyPosted(MoneyPosted e) {
        BigDecimal signed = e.direction() == Direction.CREDIT ? e.amount() : e.amount().negate();

        BigDecimal balanceAfter = jdbc.queryForObject(
                """
                UPDATE rm_account_balance
                SET balance = balance + ?, available = available + ?, updated_at = now()
                WHERE account_id = ?
                RETURNING balance
                """,
                BigDecimal.class,
                signed, signed, e.accountId());

        jdbc.update(
                """
                INSERT INTO rm_transaction_history
                    (account_id, tx_id, direction, amount, counterparty, balance_after, movement_type, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                e.accountId(),
                e.txId(),
                e.direction() == Direction.CREDIT ? "C" : "D",
                e.amount(),
                e.counterpartyAccountId(),
                balanceAfter,
                e.movementType().name(),
                java.sql.Timestamp.from(e.postedAt()));
    }

    private void onHoldPlaced(HoldPlaced e) {
        // Giữ chỗ: available giảm, balance không đổi.
        jdbc.update(
                "UPDATE rm_account_balance SET available = available - ?, updated_at = now() WHERE account_id = ?",
                e.amount(), e.accountId());
        jdbc.update(
                """
                INSERT INTO rm_hold (hold_id, account_id, amount, status, placed_at, expires_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?)
                ON CONFLICT (hold_id) DO NOTHING
                """,
                e.holdId(),
                e.accountId(),
                e.amount(),
                java.sql.Timestamp.from(e.placedAt()),
                java.sql.Timestamp.from(e.expiresAt()));
    }

    private void onHoldReleased(HoldReleased e) {
        // Nhả chỗ: trả lại available. Nếu là CAPTURED, ngay sau đó MoneyPosted(DEBIT) sẽ trừ
        // cả balance lẫn available -> available ròng không đổi (tiền vốn đã không khả dụng).
        jdbc.update(
                "UPDATE rm_account_balance SET available = available + ?, updated_at = now() WHERE account_id = ?",
                e.amount(), e.accountId());
        String status = e.reason() == HoldReleaseReason.CAPTURED ? "CAPTURED" : "RELEASED";
        jdbc.update(
                "UPDATE rm_hold SET status = ?, reason = ?, released_at = ? WHERE hold_id = ?",
                status, e.reason().name(), java.sql.Timestamp.from(e.releasedAt()), e.holdId());
    }
}
