package com.ledger.account.approval;

import java.util.UUID;

/** Yêu cầu đã được quyết định (duyệt/từ chối) rồi — không thể quyết định lại. */
public class ApprovalNotPendingException extends RuntimeException {

    public ApprovalNotPendingException(UUID approvalId, ApprovalStatus status) {
        super("Yêu cầu " + approvalId + " không còn ở trạng thái chờ duyệt (hiện: " + status + ")");
    }
}
