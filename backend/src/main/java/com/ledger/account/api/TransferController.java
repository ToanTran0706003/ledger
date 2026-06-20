package com.ledger.account.api;

import com.ledger.account.approval.MakerCheckerService;
import com.ledger.account.approval.TransferResult;
import com.ledger.account.security.AccessControl;
import com.ledger.shared.idempotency.IdempotencyService;
import com.ledger.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final MakerCheckerService makerChecker;
    private final IdempotencyService idempotency;
    private final AccessControl accessControl;
    private final CurrentUser currentUser;

    public TransferController(
            MakerCheckerService makerChecker,
            IdempotencyService idempotency,
            AccessControl accessControl,
            CurrentUser currentUser) {
        this.makerChecker = makerChecker;
        this.idempotency = idempotency;
        this.accessControl = accessControl;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TransferResult> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // Chỉ chủ tài khoản nguồn mới được chuyển đi; người tạo (maker) là người đang đăng nhập.
        accessControl.requireAccountAccess(request.fromAccountId());
        String maker = currentUser.requireUserId();
        String hash = IdempotencyService.hashOf(
                "transfer", request.fromAccountId(), request.toAccountId(), request.amount().toPlainString());
        TransferResult result = idempotency.execute(idempotencyKey, hash, TransferResult.class,
                () -> makerChecker.submitTransfer(
                        maker, request.fromAccountId(), request.toAccountId(), request.amount()));
        // Vượt ngưỡng -> chờ duyệt (202); chạy ngay -> đã tạo giao dịch (201).
        HttpStatus status = "PENDING_APPROVAL".equals(result.status()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }
}
