-- Maker-checker: yêu cầu chuyển tiền vượt ngưỡng chờ duyệt. Trạng thái CRUD -> JPA (như users,
-- standing_orders). Chỉ khi APPROVED mới phát giao dịch thật vào event store.
CREATE TABLE pending_transfers (
    id              UUID PRIMARY KEY,
    maker_user_id   VARCHAR(64) NOT NULL,
    from_account_id VARCHAR(64) NOT NULL,
    to_account_id   VARCHAR(64) NOT NULL,
    amount          NUMERIC(20,2) NOT NULL,
    status          VARCHAR(16) NOT NULL,         -- PENDING | APPROVED | REJECTED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_by      VARCHAR(64),                  -- người duyệt/từ chối (khác maker)
    decided_at      TIMESTAMPTZ,
    decision_reason VARCHAR(255),
    tx_id           VARCHAR(64)                   -- txId của giao dịch thật khi đã duyệt
);

-- Màn duyệt của ADMIN chỉ liệt kê yêu cầu đang chờ.
CREATE INDEX idx_pending_transfers_status ON pending_transfers (status, created_at);
