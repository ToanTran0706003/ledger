package com.ledger.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.iam.domain.Role;
import com.ledger.iam.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Seed admin bootstrap: tạo đúng một ADMIN, idempotent khi gọi lại. */
@SpringBootTest
@ActiveProfiles("test")
class AdminSeedServiceTest {

    @Autowired
    private AdminSeedService adminSeed;

    @Autowired
    private UserRepository users;

    @Test
    void seeds_an_admin_once_and_is_idempotent() {
        String username = "bootstrap-admin-test";
        users.findByUsername(username).ifPresent(users::delete);

        boolean firstCreated = adminSeed.seedIfAbsent(username, "secret-pw");
        boolean secondCreated = adminSeed.seedIfAbsent(username, "secret-pw");

        assertThat(firstCreated).isTrue();
        assertThat(secondCreated).isFalse(); // đã tồn tại -> không tạo lại
        assertThat(users.findByUsername(username).orElseThrow().getRole()).isEqualTo(Role.ADMIN);

        users.deleteById(users.findByUsername(username).orElseThrow().getId());
    }
}
