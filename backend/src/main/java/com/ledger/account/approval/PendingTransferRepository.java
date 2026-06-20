package com.ledger.account.approval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, UUID> {

    List<PendingTransfer> findByStatusOrderByCreatedAt(ApprovalStatus status);

    /**
     * Chuyển trạng thái NGUYÊN TỬ theo điều kiện: chỉ đổi nếu yêu cầu còn ở trạng thái {@code from}.
     * Trả số dòng cập nhật (1 = giành được, 0 = ai đó đã quyết định trước). {@code WHERE status=:from}
     * + khoá hàng của Postgres khiến hai lệnh duyệt/từ chối song song chỉ MỘT lệnh thắng — chặn
     * thực thi giao dịch hai lần (xem ADR-0021).
     */
    @Modifying
    @Transactional
    @Query("UPDATE PendingTransfer p SET p.status = :to, p.decidedBy = :by, p.decidedAt = :at, "
            + "p.decisionReason = :reason WHERE p.id = :id AND p.status = :from")
    int transition(
            @Param("id") UUID id,
            @Param("from") ApprovalStatus from,
            @Param("to") ApprovalStatus to,
            @Param("by") String by,
            @Param("at") Instant at,
            @Param("reason") String reason);

    @Modifying
    @Transactional
    @Query("UPDATE PendingTransfer p SET p.txId = :txId WHERE p.id = :id")
    int attachTxId(@Param("id") UUID id, @Param("txId") String txId);
}
