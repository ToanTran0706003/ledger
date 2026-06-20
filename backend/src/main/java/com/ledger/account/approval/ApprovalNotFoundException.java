package com.ledger.account.approval;

import java.util.UUID;

/** Không tìm thấy yêu cầu chờ duyệt với id đã cho. */
public class ApprovalNotFoundException extends RuntimeException {

    public ApprovalNotFoundException(UUID approvalId) {
        super("Không tìm thấy yêu cầu duyệt: " + approvalId);
    }
}
