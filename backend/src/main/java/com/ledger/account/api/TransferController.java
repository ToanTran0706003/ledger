package com.ledger.account.api;

import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.TransferCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final MoneyMovementHandler moneyMovement;

    public TransferController(MoneyMovementHandler moneyMovement) {
        this.moneyMovement = moneyMovement;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        String txId = moneyMovement.transfer(
                new TransferCommand(request.fromAccountId(), request.toAccountId(), request.amount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(txId));
    }
}
