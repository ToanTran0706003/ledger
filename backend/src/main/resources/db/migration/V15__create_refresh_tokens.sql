-- Whitelist refresh token (rotation + thu hồi). Mỗi hàng = một refresh token còn hiệu lực;
-- id = jti trong token. Refresh single-use: tiêu thụ (xoá) hàng cũ rồi cấp jti mới. Logout xoá
-- toàn bộ hàng của user. Token bị lộ/đã dùng lại -> jti không còn trong bảng -> từ chối (ADR-0021).
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
