package com.ledger.account.api;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.WithdrawCommand;
import com.ledger.account.query.AccountBalanceView;
import com.ledger.account.query.AccountQueryService;
import com.ledger.account.query.TransactionHistoryView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final OpenAccountCommandHandler openAccount;
    private final MoneyMovementHandler moneyMovement;
    private final AccountQueryService accountQuery;

    public AccountController(
            OpenAccountCommandHandler openAccount,
            MoneyMovementHandler moneyMovement,
            AccountQueryService accountQuery) {
        this.openAccount = openAccount;
        this.moneyMovement = moneyMovement;
        this.accountQuery = accountQuery;
    }

    @PostMapping
    public ResponseEntity<OpenAccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        String accountId = openAccount.handle(new OpenAccountCommand(request.owner(), request.type()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new OpenAccountResponse(accountId));
    }

    @PostMapping("/{accountId}/deposit")
    public TransactionResponse deposit(@PathVariable String accountId, @Valid @RequestBody AmountRequest request) {
        return new TransactionResponse(moneyMovement.deposit(new DepositCommand(accountId, request.amount())));
    }

    @PostMapping("/{accountId}/withdraw")
    public TransactionResponse withdraw(@PathVariable String accountId, @Valid @RequestBody AmountRequest request) {
        return new TransactionResponse(moneyMovement.withdraw(new WithdrawCommand(accountId, request.amount())));
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceView balance(@PathVariable String accountId) {
        return accountQuery
                .findBalance(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    @GetMapping("/{accountId}/history")
    public List<TransactionHistoryView> history(@PathVariable String accountId) {
        return accountQuery.findHistory(accountId);
    }
}
