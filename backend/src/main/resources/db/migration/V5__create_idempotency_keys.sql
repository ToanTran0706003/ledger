-- Idempotency store: đảm bảo cùng một lệnh ghi (cùng Idempotency-Key) chỉ chạy một lần.
-- request_hash phát hiện việc dùng lại key cho payload khác (lỗi client).
CREATE TABLE idempotency_keys (
    idem_key      VARCHAR(128) PRIMARY KEY,
    request_hash  VARCHAR(64) NOT NULL,
    response_body JSONB,
    status        VARCHAR(16) NOT NULL,        -- IN_PROGRESS / COMPLETED
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ
);
