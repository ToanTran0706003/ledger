package com.ledger.shared.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rate limiting end-to-end qua HTTP: đăng nhập sai nhiều lần từ một IP bị chặn (429) sau khi
 * cạn xô token — bảo vệ chống dò mật khẩu. Ngưỡng đặt thấp + không nạp lại để tất định.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "ledger.rate-limit.enabled=true",
            "ledger.rate-limit.auth.capacity=3",
            "ledger.rate-limit.auth.refill-per-minute=0"
        })
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void login_attempts_from_one_ip_are_rate_limited() throws Exception {
        String body = mapper.writeValueAsString(Map.of("username", "nobody", "password", "wrong"));

        // 3 lần đầu được xử lý (sai mật khẩu -> 401); lần thứ 4 vượt giới hạn -> 429.
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }
}
