package com.ledger.iam;

import com.ledger.iam.api.AuthDtos.TwoFactorSetupResponse;
import com.ledger.iam.domain.UserAccount;
import com.ledger.iam.domain.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý 2FA (TOTP) cho người dùng: ghi danh (sinh secret), bật (xác nhận bằng mã hợp lệ), tắt,
 * và tra trạng thái. Việc kiểm mã lúc đăng nhập nằm ở {@link AuthService}.
 */
@Service
public class TwoFactorService {

    private final UserRepository users;
    private final TotpService totp;

    public TwoFactorService(UserRepository users, TotpService totp) {
        this.users = users;
        this.totp = totp;
    }

    /** Bắt đầu ghi danh: sinh secret mới (chưa bật) và trả về secret + URI otpauth để quét QR. */
    @Transactional
    public TwoFactorSetupResponse setup(UUID userId) {
        UserAccount user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        String secret = totp.generateSecret();
        user.startTotpEnrollment(secret);
        users.save(user);
        return new TwoFactorSetupResponse(secret, totp.otpauthUri(secret, user.getUsername()));
    }

    /** Bật 2FA sau khi người dùng xác nhận bằng một mã hợp lệ (chứng tỏ đã thêm đúng vào app). */
    @Transactional
    public void enable(UUID userId, String code) {
        UserAccount user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.getTotpSecret() == null || !totp.verify(user.getTotpSecret(), code)) {
            throw new InvalidTwoFactorCodeException();
        }
        user.confirmTotp();
        users.save(user);
    }

    /** Tắt 2FA — yêu cầu một mã hợp lệ để tránh người khác tắt khi chiếm phiên. */
    @Transactional
    public void disable(UUID userId, String code) {
        UserAccount user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!user.isTotpEnabled() || !totp.verify(user.getTotpSecret(), code)) {
            throw new InvalidTwoFactorCodeException();
        }
        user.disableTotp();
        users.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return users.findById(userId).map(UserAccount::isTotpEnabled).orElse(false);
    }
}
