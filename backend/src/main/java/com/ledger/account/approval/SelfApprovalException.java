package com.ledger.account.approval;

import java.util.UUID;

/** Vi phạm nguyên tắc bốn-mắt: người tạo yêu cầu không được tự duyệt chính nó. */
public class SelfApprovalException extends RuntimeException {

    public SelfApprovalException(UUID approvalId) {
        super("Không được tự duyệt yêu cầu của chính mình (nguyên tắc bốn-mắt): " + approvalId);
    }
}
