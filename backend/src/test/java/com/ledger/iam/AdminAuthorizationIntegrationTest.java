package com.ledger.iam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledger.account.command.OpenAccountCommand;
import com.ledger.account.command.OpenAccountCommandHandler;
import com.ledger.account.domain.AccountType;
import com.ledger.iam.domain.Role;
import com.ledger.iam.domain.UserAccount;
import com.ledger.iam.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phân quyền các endpoint nhạy cảm theo vai trò (04-security mục 3): /audit/hash-chain cho
 * ADMIN/AUDITOR; /admin/** chỉ ADMIN. Chứng minh CUSTOMER bị chặn 403, AUDITOR bị chặn khỏi
 * /admin nhưng xem được audit, ADMIN toàn quyền — ranh giới bảo mật thực thi ở server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwt;

    @Autowired
    private OpenAccountCommandHandler openAccount;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("TRUNCATE TABLE users");
        jdbc.update("TRUNCATE TABLE events RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE snapshots");
        jdbc.update("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE idempotency_keys");
        jdbc.update("TRUNCATE TABLE rm_transaction_history RESTART IDENTITY");
        jdbc.update("TRUNCATE TABLE rm_account_balance");
        jdbc.update("TRUNCATE TABLE rm_hold");
    }

    private String tokenFor(String username, Role role) {
        UserAccount user =
                new UserAccount(UUID.randomUUID(), username, passwordEncoder.encode("pw"), role, Instant.now());
        users.save(user);
        return jwt.issueAccessToken(user);
    }

    @Test
    void hash_chain_verify_requires_admin_or_auditor() throws Exception {
        mvc.perform(get("/audit/hash-chain")).andExpect(status().isUnauthorized());
        mvc.perform(bearer(get("/audit/hash-chain"), tokenFor("cust", Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        mvc.perform(bearer(get("/audit/hash-chain"), tokenFor("auditor", Role.AUDITOR)))
                .andExpect(status().isOk());
        mvc.perform(bearer(get("/audit/hash-chain"), tokenFor("admin", Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void frozen_accounts_list_is_admin_only() throws Exception {
        mvc.perform(bearer(get("/admin/fraud/frozen"), tokenFor("cust", Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        // AUDITOR xem được audit nhưng KHÔNG vào được /admin.
        mvc.perform(bearer(get("/admin/fraud/frozen"), tokenFor("auditor", Role.AUDITOR)))
                .andExpect(status().isForbidden());
        mvc.perform(bearer(get("/admin/fraud/frozen"), tokenFor("admin", Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void only_admin_can_freeze_and_unfreeze() throws Exception {
        String accountId = openAccount.handle(new OpenAccountCommand("owner-1", AccountType.CUSTOMER));
        String freezeBody = "{\"reason\":\"kiểm tra phân quyền\"}";

        // CUSTOMER bị chặn ngay ở tầng security (403), không tới handler.
        mvc.perform(bearer(post("/admin/accounts/" + accountId + "/freeze"), tokenFor("cust", Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isForbidden());

        // ADMIN đóng băng rồi mở băng thành công.
        String admin = tokenFor("admin", Role.ADMIN);
        mvc.perform(bearer(post("/admin/accounts/" + accountId + "/freeze"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isNoContent());
        mvc.perform(bearer(post("/admin/accounts/" + accountId + "/unfreeze"), admin))
                .andExpect(status().isNoContent());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bearer(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
