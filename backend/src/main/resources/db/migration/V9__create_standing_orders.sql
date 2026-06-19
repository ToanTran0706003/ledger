-- Lệnh chuyển tiền định kỳ (standing order). Trạng thái CRUD thường -> JPA (như users).
CREATE TABLE standing_orders (
    id               UUID PRIMARY KEY,
    owner_user_id    VARCHAR(64) NOT NULL,
    from_account_id  VARCHAR(64) NOT NULL,
    to_account_id    VARCHAR(64) NOT NULL,
    amount           NUMERIC(20,2) NOT NULL,
    interval_seconds BIGINT NOT NULL,
    next_run_at      TIMESTAMPTZ NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index một phần: scheduler chỉ quét lệnh còn hoạt động, theo thời điểm chạy kế tiếp.
CREATE INDEX idx_standing_due ON standing_orders (next_run_at) WHERE active;
