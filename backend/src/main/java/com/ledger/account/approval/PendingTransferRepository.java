package com.ledger.account.approval;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, UUID> {

    List<PendingTransfer> findByStatusOrderByCreatedAt(ApprovalStatus status);
}
