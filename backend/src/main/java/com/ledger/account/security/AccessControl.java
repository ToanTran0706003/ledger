package com.ledger.account.security;

import com.ledger.account.query.AccountBalanceView;
import com.ledger.account.query.AccountQueryService;
import com.ledger.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ownership check: mọi truy cập một accountId phải thuộc về user đang đăng nhập, trừ
 * khi user là ADMIN (04-security mục 3). Ngăn rò rỉ chéo tài khoản.
 */
@Component
public class AccessControl {

    private final AccountQueryService accountQuery;
    private final CurrentUser currentUser;

    public AccessControl(AccountQueryService accountQuery, CurrentUser currentUser) {
        this.accountQuery = accountQuery;
        this.currentUser = currentUser;
    }

    public void requireAccountAccess(String accountId) {
        if (currentUser.isAdmin()) {
            return;
        }
        String userId = currentUser.requireUserId();
        AccountBalanceView view = accountQuery
                .findBalance(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
        if (!userId.equals(view.owner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập tài khoản này");
        }
    }
}
