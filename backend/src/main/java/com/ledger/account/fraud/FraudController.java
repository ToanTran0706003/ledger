package com.ledger.account.fraud;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giám sát gian lận của ADMIN (theo /admin/** trong SecurityConfig): liệt kê tài khoản bị đóng
 * băng, đóng băng/mở băng thủ công. Tự đóng băng do luật chạy trong FraudService sau mỗi ghi nợ.
 */
@RestController
@RequestMapping("/admin")
public class FraudController {

    private final FraudService fraudService;
    private final FraudQueryService fraudQuery;

    public FraudController(FraudService fraudService, FraudQueryService fraudQuery) {
        this.fraudService = fraudService;
        this.fraudQuery = fraudQuery;
    }

    @GetMapping("/fraud/frozen")
    public List<FrozenAccountView> frozen() {
        return fraudQuery.listFrozen();
    }

    @PostMapping("/accounts/{accountId}/freeze")
    public ResponseEntity<Void> freeze(@PathVariable String accountId, @Valid @RequestBody FreezeRequest request) {
        fraudService.freeze(accountId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{accountId}/unfreeze")
    public ResponseEntity<Void> unfreeze(@PathVariable String accountId) {
        fraudService.unfreeze(accountId);
        return ResponseEntity.noContent().build();
    }
}
