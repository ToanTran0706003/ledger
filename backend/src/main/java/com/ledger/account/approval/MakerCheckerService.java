package com.ledger.account.approval;

import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.TransferCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Maker-checker (nguyên tắc bốn-mắt): chuyển tiền vượt ngưỡng phải được một người DUYỆT KHÁC
 * người tạo phê duyệt trước khi thực thi. Dưới ngưỡng thì chạy ngay. Tách "người tạo" (maker) và
 * "người duyệt" (checker) là kiểm soát phân tách nhiệm vụ chuẩn của ngân hàng.
 *
 * Tắt được qua {@code ledger.maker-checker.enabled} (mặc định tắt trong test).
 */
@Service
public class MakerCheckerService {

    private final MoneyMovementHandler money;
    private final PendingTransferRepository repository;
    private final boolean enabled;
    private final BigDecimal threshold;

    public MakerCheckerService(
            MoneyMovementHandler money,
            PendingTransferRepository repository,
            @Value("${ledger.maker-checker.enabled:false}") boolean enabled,
            @Value("${ledger.maker-checker.threshold:100000000}") BigDecimal threshold) {
        this.money = money;
        this.repository = repository;
        this.enabled = enabled;
        this.threshold = threshold;
    }

    /** Nộp lệnh chuyển tiền: chạy ngay nếu dưới ngưỡng, ngược lại tạo yêu cầu chờ duyệt. */
    public TransferResult submitTransfer(String makerUserId, String fromAccountId, String toAccountId, BigDecimal amount) {
        if (!enabled || amount.compareTo(threshold) < 0) {
            return TransferResult.executed(money.transfer(new TransferCommand(fromAccountId, toAccountId, amount)));
        }
        PendingTransfer pending =
                new PendingTransfer(UUID.randomUUID(), makerUserId, fromAccountId, toAccountId, amount, Instant.now());
        repository.save(pending);
        return TransferResult.pending(pending.getId());
    }

    /** Duyệt một yêu cầu (người duyệt phải KHÁC người tạo) -> thực thi giao dịch thật. Trả về txId. */
    public String approve(String approverUserId, UUID approvalId) {
        PendingTransfer pending = find(approvalId);
        requirePending(pending);
        if (approverUserId.equals(pending.getMakerUserId())) {
            throw new SelfApprovalException(approvalId);
        }
        String txId = money.transfer(
                new TransferCommand(pending.getFromAccountId(), pending.getToAccountId(), pending.getAmount()));
        pending.markApproved(approverUserId, txId, Instant.now());
        repository.save(pending);
        return txId;
    }

    /** Từ chối một yêu cầu — không thực thi giao dịch. */
    public void reject(String approverUserId, UUID approvalId, String reason) {
        PendingTransfer pending = find(approvalId);
        requirePending(pending);
        pending.markRejected(approverUserId, reason, Instant.now());
        repository.save(pending);
    }

    public List<PendingTransfer> listPending() {
        return repository.findByStatusOrderByCreatedAt(ApprovalStatus.PENDING);
    }

    public PendingTransfer find(UUID approvalId) {
        return repository.findById(approvalId).orElseThrow(() -> new ApprovalNotFoundException(approvalId));
    }

    private static void requirePending(PendingTransfer pending) {
        if (pending.getStatus() != ApprovalStatus.PENDING) {
            throw new ApprovalNotPendingException(pending.getId(), pending.getStatus());
        }
    }
}
