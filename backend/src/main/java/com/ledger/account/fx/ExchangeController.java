package com.ledger.account.fx;

import com.ledger.account.api.TransactionResponse;
import com.ledger.account.security.AccessControl;
import com.ledger.shared.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quy đổi tiền tệ giữa hai tài khoản của người dùng. Cần quyền với cả hai tài khoản (ownership);
 * tỉ giá do hệ thống cấu hình; Idempotency-Key chống double-submit (đây là thao tác ghi tiền).
 */
@RestController
@RequestMapping("/exchanges")
public class ExchangeController {

    private final ExchangeHandler exchange;
    private final AccessControl accessControl;
    private final IdempotencyService idempotency;

    public ExchangeController(
            ExchangeHandler exchange, AccessControl accessControl, IdempotencyService idempotency) {
        this.exchange = exchange;
        this.accessControl = accessControl;
        this.idempotency = idempotency;
    }

    @PostMapping
    public TransactionResponse exchange(
            @Valid @RequestBody ExchangeRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        accessControl.requireAccountAccess(request.fromAccountId());
        accessControl.requireAccountAccess(request.toAccountId());
        String hash = IdempotencyService.hashOf(
                "exchange", request.fromAccountId(), request.toAccountId(), request.amount().toPlainString());
        return idempotency.execute(idempotencyKey, hash, TransactionResponse.class, () -> new TransactionResponse(
                exchange.exchange(request.fromAccountId(), request.toAccountId(), request.amount())));
    }
}
