package com.ledger.iam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Bảo mật end-to-end qua HTTP: cần token để truy cập; không truy cập chéo tài khoản
 * (ownership); endpoint /audit cần vai trò; sai mật khẩu / trùng username xử lý đúng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

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
    }

    private String json(Map<String, String> body) throws Exception {
        return mapper.writeValueAsString(body);
    }

    private String registerAndGetToken(String username) throws Exception {
        MvcResult res = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String openAccount(String token) throws Exception {
        MvcResult res = mvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "CUSTOMER"))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("accountId").asText();
    }

    @Test
    void protected_endpoint_requires_token() throws Exception {
        mvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "CUSTOMER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_then_open_account_with_token() throws Exception {
        String token = registerAndGetToken("alice");
        openAccount(token); // 201 trong helper
    }

    @Test
    void cannot_access_other_users_account() throws Exception {
        String aliceToken = registerAndGetToken("alice");
        String accountId = openAccount(aliceToken);

        // Chủ tài khoản xem được.
        mvc.perform(get("/accounts/" + accountId + "/balance").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        // Người khác bị từ chối (403).
        String bobToken = registerAndGetToken("bob");
        mvc.perform(get("/accounts/" + accountId + "/balance").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_can_read_integrity() throws Exception {
        // Integrity là chỉ số minh bạch -> mọi user đã đăng nhập xem được.
        String token = registerAndGetToken("alice");
        mvc.perform(get("/audit/integrity").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void customer_cannot_reverse_transaction() throws Exception {
        // Reverse chỉ dành cho ADMIN -> CUSTOMER bị từ chối (403) ngay ở tầng security.
        String token = registerAndGetToken("alice");
        mvc.perform(post("/transactions/some-tx/reverse")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "k-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_with_wrong_password_is_unauthorized() throws Exception {
        registerAndGetToken("alice");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "alice", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicate_username_is_conflict() throws Exception {
        registerAndGetToken("alice");
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "alice", "password", "password123"))))
                .andExpect(status().isConflict());
    }
}
