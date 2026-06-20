package com.ledger.account.approval;

import java.util.UUID;

/**
 * Kết quả nộp một lệnh chuyển tiền: hoặc đã thực thi ngay ({@code EXECUTED} + txId), hoặc bị giữ
 * lại chờ duyệt ({@code PENDING_APPROVAL} + approvalId) khi vượt ngưỡng maker-checker.
 */
public record TransferResult(String status, String txId, UUID approvalId) {

    public static TransferResult executed(String txId) {
        return new TransferResult("EXECUTED", txId, null);
    }

    public static TransferResult pending(UUID approvalId) {
        return new TransferResult("PENDING_APPROVAL", null, approvalId);
    }
}
