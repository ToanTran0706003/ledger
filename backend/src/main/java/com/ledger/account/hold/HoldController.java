package com.ledger.account.hold;

import com.ledger.account.api.TransactionResponse;
import com.ledger.account.domain.HoldReleaseReason;
import com.ledger.account.security.AccessControl;
import com.ledger.shared.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hold/reservation trên một tài khoản: đặt giữ (giảm available), liệt kê, nhả, và thu (capture).
 * Mọi truy cập đều qua ownership check. Đặt giữ và thu tác động tới tiền nên dùng Idempotency-Key
 * (chống double-submit), giống nạp/rút (ADR-0007).
 */
@RestController
@RequestMapping("/accounts/{accountId}/holds")
public class HoldController {

    private final HoldService holdService;
    private final HoldQueryService holdQuery;
    private final IdempotencyService idempotency;
    private final AccessControl accessControl;

    public HoldController(
            HoldService holdService,
            HoldQueryService holdQuery,
            IdempotencyService idempotency,
            AccessControl accessControl) {
        this.holdService = holdService;
        this.holdQuery = holdQuery;
        this.idempotency = idempotency;
        this.accessControl = accessControl;
    }

    @PostMapping
    public ResponseEntity<HoldResponse> place(
            @PathVariable String accountId,
            @Valid @RequestBody PlaceHoldRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        accessControl.requireAccountAccess(accountId);
        String hash = IdempotencyService.hashOf(
                "hold-place", accountId, request.amount().toPlainString(), String.valueOf(request.ttlSeconds()));
        HoldResponse response = idempotency.execute(idempotencyKey, hash, HoldResponse.class, () -> new HoldResponse(
                holdService.place(accountId, request.amount(), Duration.ofSeconds(request.ttlSeconds()))));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<HoldView> list(@PathVariable String accountId) {
        accessControl.requireAccountAccess(accountId);
        return holdQuery.findByAccount(accountId);
    }

    @PostMapping("/{holdId}/release")
    public ResponseEntity<Void> release(@PathVariable String accountId, @PathVariable String holdId) {
        accessControl.requireAccountAccess(accountId);
        holdService.release(accountId, holdId, HoldReleaseReason.MANUAL);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{holdId}/capture")
    public TransactionResponse capture(
            @PathVariable String accountId,
            @PathVariable String holdId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        accessControl.requireAccountAccess(accountId);
        String hash = IdempotencyService.hashOf("hold-capture", accountId, holdId);
        return idempotency.execute(idempotencyKey, hash, TransactionResponse.class, () -> new TransactionResponse(
                holdService.capture(accountId, holdId)));
    }
}
