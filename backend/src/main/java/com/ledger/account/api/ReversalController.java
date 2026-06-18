package com.ledger.account.api;

import com.ledger.account.command.ReverseTransactionCommand;
import com.ledger.account.command.ReverseTransactionHandler;
import com.ledger.shared.idempotency.IdempotencyService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class ReversalController {

    private final ReverseTransactionHandler reverseHandler;
    private final IdempotencyService idempotency;

    public ReversalController(ReverseTransactionHandler reverseHandler, IdempotencyService idempotency) {
        this.reverseHandler = reverseHandler;
        this.idempotency = idempotency;
    }

    @PostMapping("/{txId}/reverse")
    public TransactionResponse reverse(
            @PathVariable String txId, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        String hash = IdempotencyService.hashOf("reverse", txId);
        return idempotency.execute(idempotencyKey, hash, TransactionResponse.class,
                () -> new TransactionResponse(reverseHandler.reverse(new ReverseTransactionCommand(txId))));
    }
}
