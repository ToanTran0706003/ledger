package com.ledger.iam;

import com.ledger.iam.api.AuthDtos.TokenResponse;
import com.ledger.iam.domain.RefreshTokenRecord;
import com.ledger.iam.domain.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final TotpService totp;
    // Hash giả để băm "tốn thời gian" ngay cả khi username không tồn tại -> san bằng thời gian phản
    // hồi, chống liệt kê username qua timing (audit #5). Tính một lần lúc khởi động.
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            TotpService totp) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.totp = totp;
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

    @Transactional
    public TokenResponse login(String username, String rawPassword) {
        return login(username, rawPassword, null);
    }

    @Transactional
    public TokenResponse login(String username, String rawPassword, String totpCode) {
        UserAccount user = users.findByUsername(username).orElse(null);
        // Luôn chạy BCrypt (với hash thật hoặc hash giả) để thời gian phản hồi không tiết lộ
        // username có tồn tại hay không. Thông điệp lỗi cũng đồng nhất cho cả hai nhánh.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        // Lớp thứ hai: nếu đã bật 2FA, bắt buộc mã TOTP hợp lệ mới cấp token.
        if (user.isTotpEnabled()) {
            if (totpCode == null || totpCode.isBlank()) {
                throw new TwoFactorRequiredException();
            }
            if (!totp.verify(user.getTotpSecret(), totpCode)) {
                throw new InvalidTwoFactorCodeException();
            }
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Jwt decoded;
        try {
            decoded = jwt.verifyRefreshToken(refreshToken);
        } catch (RuntimeException e) {
            throw new InvalidCredentialsException();
        }
        // Rotation single-use: tiêu thụ jti cũ một cách nguyên tử. Không còn trong whitelist (đã dùng,
        // đã thu hồi khi logout, hoặc bị giả mạo) -> từ chối. Mỗi refresh cấp một jti mới.
        String jtiClaim = decoded.getId();
        if (jtiClaim == null || refreshTokens.consume(UUID.fromString(jtiClaim)) == 0) {
            throw new InvalidCredentialsException();
        }
        UserAccount user = users.findById(UUID.fromString(decoded.getSubject()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    /** Thu hồi mọi refresh token của user (logout) -> các refresh token cũ hết hiệu lực ngay. */
    @Transactional
    public void logout(UUID userId) {
        refreshTokens.deleteByUserId(userId);
    }

    private TokenResponse issueTokens(UserAccount user) {
        UUID jti = UUID.randomUUID();
        refreshTokens.save(new RefreshTokenRecord(jti, user.getId(), jwt.refreshExpiry(), Instant.now()));
        return new TokenResponse(jwt.issueAccessToken(user), jwt.issueRefreshToken(user, jti), "Bearer");
    }
}
