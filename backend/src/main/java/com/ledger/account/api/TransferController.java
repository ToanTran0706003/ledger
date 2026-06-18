package com.ledger.account.api;

import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.TransferCommand;
import com.ledger.account.security.AccessControl;
import com.ledger.shared.idempotency.IdempotencyService;
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

    private final MoneyMovementHandler moneyMovement;
    private final IdempotencyService idempotency;
    private final AccessControl accessControl;

    public TransferController(
            MoneyMovementHandler moneyMovement, IdempotencyService idempotency, AccessControl accessControl) {
        this.moneyMovement = moneyMovement;
        this.idempotency = idempotency;
        this.accessControl = accessControl;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // Chỉ chủ tài khoản nguồn mới được chuyển đi.
        accessControl.requireAccountAccess(request.fromAccountId());
        String hash = IdempotencyService.hashOf(
                "transfer", request.fromAccountId(), request.toAccountId(), request.amount().toPlainString());
        TransactionResponse response = idempotency.execute(idempotencyKey, hash, TransactionResponse.class,
                () -> new TransactionResponse(moneyMovement.transfer(
                        new TransferCommand(request.fromAccountId(), request.toAccountId(), request.amount()))));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
