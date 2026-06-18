-- Read model: lịch sử giao dịch, phục vụ hiển thị sao kê.
-- Mỗi posting (MoneyPosted) tạo một dòng ở đây.
CREATE TABLE rm_transaction_history (
    id            BIGSERIAL PRIMARY KEY,
    account_id    VARCHAR(64) NOT NULL,
    tx_id         VARCHAR(64) NOT NULL,
    direction     CHAR(1) NOT NULL,            -- D (debit) / C (credit)
    amount        NUMERIC(20,2) NOT NULL,
    counterparty  VARCHAR(64),
    balance_after NUMERIC(20,2) NOT NULL,
    movement_type VARCHAR(16) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rm_history_account ON rm_transaction_history (account_id, occurred_at DESC);
