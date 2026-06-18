package com.ledger.account.api;

import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.query.AccountBalanceView;
import com.ledger.account.query.AccountQueryService;
import jakarta.validation.Valid;
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
    private final AccountQueryService accountQuery;

    public AccountController(OpenAccountCommandHandler openAccount, AccountQueryService accountQuery) {
        this.openAccount = openAccount;
        this.accountQuery = accountQuery;
    }

    @PostMapping
    public ResponseEntity<OpenAccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        String accountId = openAccount.handle(new OpenAccountCommand(request.owner(), request.type()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new OpenAccountResponse(accountId));
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceView balance(@PathVariable String accountId) {
        return accountQuery
                .findBalance(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }
}
