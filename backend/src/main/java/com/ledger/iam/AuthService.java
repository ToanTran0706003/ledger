package com.ledger.iam;

import com.ledger.iam.api.AuthDtos.TokenResponse;
import com.ledger.iam.domain.Role;
import com.ledger.iam.domain.UserAccount;
import com.ledger.iam.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đăng ký, đăng nhập, làm mới token. Mật khẩu băm BCrypt, không bao giờ lưu plaintext. */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    // Hash giả để băm "tốn thời gian" ngay cả khi username không tồn tại -> san bằng thời gian phản
    // hồi, chống liệt kê username qua timing (audit #5). Tính một lần lúc khởi động.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.dummyHash = passwordEncoder.encode("ledger-no-such-user-placeholder");
    }

    @Transactional
    public TokenResponse register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        UserAccount user = new UserAccount(
                UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), Role.CUSTOMER, Instant.now());
        users.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String username, String rawPassword) {
        UserAccount user = users.findByUsername(username).orElse(null);
        // Luôn chạy BCrypt (với hash thật hoặc hash giả) để thời gian phản hồi không tiết lộ
        // username có tồn tại hay không. Thông điệp lỗi cũng đồng nhất cho cả hai nhánh.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Jwt decoded;
        try {
            decoded = jwt.verifyRefreshToken(refreshToken);
        } catch (RuntimeException e) {
            throw new InvalidCredentialsException();
        }
        UserAccount user = users.findById(UUID.fromString(decoded.getSubject()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    private TokenResponse issueTokens(UserAccount user) {
        return new TokenResponse(jwt.issueAccessToken(user), jwt.issueRefreshToken(user), "Bearer");
    }
}
