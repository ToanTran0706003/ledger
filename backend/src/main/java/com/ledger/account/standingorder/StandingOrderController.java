package com.ledger.account.standingorder;

import com.ledger.account.security.AccessControl;
import com.ledger.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/standing-orders")
public class StandingOrderController {

    private final StandingOrderService service;
    private final AccessControl accessControl;
    private final CurrentUser currentUser;

    public StandingOrderController(StandingOrderService service, AccessControl accessControl, CurrentUser currentUser) {
        this.service = service;
        this.accessControl = accessControl;
        this.currentUser = currentUser;
    }

    public record CreateRequest(
            @NotBlank String fromAccountId,
            @NotBlank String toAccountId,
            @NotNull @Positive BigDecimal amount,
            @Positive long intervalSeconds) {}

    public record CreatedResponse(String id) {}

    public record StandingOrderView(
            String id,
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            long intervalSeconds,
            Instant nextRunAt,
            boolean active) {}

    @PostMapping
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody CreateRequest request) {
        accessControl.requireAccountAccess(request.fromAccountId()); // chỉ chủ tài khoản nguồn
        var id = service.create(
                currentUser.requireUserId(),
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.intervalSeconds());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id.toString()));
    }

    @GetMapping
    public List<StandingOrderView> myOrders() {
        return service.listFor(currentUser.requireUserId()).stream()
                .map(o -> new StandingOrderView(
                        o.getId().toString(),
                        o.getFromAccountId(),
                        o.getToAccountId(),
                        o.getAmount(),
                        o.getIntervalSeconds(),
                        o.getNextRunAt(),
                        o.isActive()))
                .toList();
    }
}
