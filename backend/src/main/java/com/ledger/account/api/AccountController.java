package com.ledger.account.api;

import com.ledger.account.command.DepositCommand;
import com.ledger.account.command.MoneyMovementHandler;
import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.command.WithdrawCommand;
import com.ledger.account.query.AccountBalanceView;
import com.ledger.account.query.AccountQueryService;
import com.ledger.account.query.BalanceAtView;
import com.ledger.account.query.TimeTravelQueryService;
import com.ledger.account.query.TransactionHistoryView;
import com.ledger.account.security.AccessControl;
import com.ledger.shared.idempotency.IdempotencyService;
import com.ledger.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final OpenAccountCommandHandler openAccount;
    private final MoneyMovementHandler moneyMovement;
    private final AccountQueryService accountQuery;
    private final TimeTravelQueryService timeTravel;
    private final IdempotencyService idempotency;
    private final AccessControl accessControl;
    private final CurrentUser currentUser;

    public AccountController(
            OpenAccountCommandHandler openAccount,
            MoneyMovementHandler moneyMovement,
            AccountQueryService accountQuery,
            TimeTravelQueryService timeTravel,
            IdempotencyService idempotency,
            AccessControl accessControl,
            CurrentUser currentUser) {
        this.openAccount = openAccount;
        this.moneyMovement = moneyMovement;
        this.accountQuery = accountQuery;
        this.timeTravel = timeTravel;
        this.idempotency = idempotency;
        this.accessControl = accessControl;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<OpenAccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        // Chủ tài khoản là người dùng đang đăng nhập (không nhận từ body).
        String accountId = openAccount.handle(new OpenAccountCommand(currentUser.requireUserId(), request.type()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new OpenAccountResponse(accountId));
    }

    @PostMapping("/{accountId}/deposit")
    public TransactionResponse deposit(
            @PathVariable String accountId,
            @Valid @RequestBody AmountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        accessControl.requireAccountAccess(accountId);
        String hash = IdempotencyService.hashOf("deposit", accountId, request.amount().toPlainString());
        return idempotency.execute(idempotencyKey, hash, TransactionResponse.class,
                () -> new TransactionResponse(moneyMovement.deposit(new DepositCommand(accountId, request.amount()))));
    }

    @PostMapping("/{accountId}/withdraw")
    public TransactionResponse withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody AmountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        accessControl.requireAccountAccess(accountId);
        String hash = IdempotencyService.hashOf("withdraw", accountId, request.amount().toPlainString());
        return idempotency.execute(idempotencyKey, hash, TransactionResponse.class,
                () -> new TransactionResponse(moneyMovement.withdraw(new WithdrawCommand(accountId, request.amount()))));
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceView balance(@PathVariable String accountId) {
        accessControl.requireAccountAccess(accountId);
        return accountQuery
                .findBalance(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    /** Time-travel: số dư tại một thời điểm (ISO-8601, vd 2026-06-18T10:00:00Z). */
    @GetMapping(value = "/{accountId}/balance", params = "asOf")
    public BalanceAtView balanceAsOf(@PathVariable String accountId, @RequestParam String asOf) {
        accessControl.requireAccountAccess(accountId);
        Instant instant = parseInstant(asOf);
        var balance = timeTravel
                .balanceAsOf(accountId, instant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không có dữ liệu tài khoản tại thời điểm này"));
        return new BalanceAtView(accountId, instant, balance);
    }

    @GetMapping("/{accountId}/history")
    public List<TransactionHistoryView> history(@PathVariable String accountId) {
        accessControl.requireAccountAccess(accountId);
        return accountQuery.findHistory(accountId);
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("asOf phải là ISO-8601 (vd 2026-06-18T10:00:00Z)");
        }
    }
}
