package com.ledger.account.approval;

import com.ledger.account.api.TransactionResponse;
import com.ledger.shared.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Duyệt giao dịch chờ phê duyệt (maker-checker) — của ADMIN (theo /admin/** trong SecurityConfig).
 * Người duyệt phải KHÁC người tạo (nguyên tắc bốn-mắt, thực thi trong MakerCheckerService).
 */
@RestController
@RequestMapping("/admin/approvals")
public class ApprovalController {

    private final MakerCheckerService makerChecker;
    private final CurrentUser currentUser;

    public ApprovalController(MakerCheckerService makerChecker, CurrentUser currentUser) {
        this.makerChecker = makerChecker;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<PendingTransferView> pending() {
        return makerChecker.listPending().stream().map(PendingTransferView::of).toList();
    }

    @PostMapping("/{id}/approve")
    public TransactionResponse approve(@PathVariable UUID id) {
        return new TransactionResponse(makerChecker.approve(currentUser.requireUserId(), id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id, @RequestBody(required = false) RejectRequest body) {
        makerChecker.reject(currentUser.requireUserId(), id, body != null ? body.reason() : null);
        return ResponseEntity.noContent().build();
    }

    public record RejectRequest(String reason) {}
}
