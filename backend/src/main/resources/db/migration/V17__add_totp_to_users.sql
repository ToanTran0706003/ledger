-- Xác thực hai lớp (2FA) bằng TOTP (RFC 6238). totp_secret: bí mật Base32 (null = chưa ghi danh);
-- totp_enabled: đã bật 2FA chưa. Secret có nhưng chưa enabled = đang ghi danh, chờ xác nhận.
ALTER TABLE users ADD COLUMN totp_secret  VARCHAR(64);
ALTER TABLE users ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT false;
