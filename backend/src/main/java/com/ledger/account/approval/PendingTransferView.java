package com.ledger.account.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Một yêu cầu chuyển tiền chờ duyệt (cho màn duyệt của ADMIN). */
public record PendingTransferView(
        UUID id,
        String makerUserId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        Instant createdAt) {

    static PendingTransferView of(PendingTransfer p) {
        return new PendingTransferView(
                p.getId(), p.getMakerUserId(), p.getFromAccountId(), p.getToAccountId(), p.getAmount(), p.getCreatedAt());
    }
}
