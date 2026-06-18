-- Read model: số dư tài khoản, tối ưu cho đọc nhanh.
-- Đây là dữ liệu PHÁI SINH — có thể xóa và dựng lại từ event store bất cứ lúc nào.
CREATE TABLE rm_account_balance (
    account_id     VARCHAR(64) PRIMARY KEY,
    owner          VARCHAR(128),
    currency       CHAR(3) NOT NULL DEFAULT 'VND',
    balance        NUMERIC(20,2) NOT NULL DEFAULT 0,
    available      NUMERIC(20,2) NOT NULL DEFAULT 0,   -- số dư khả dụng = balance - các hold (Phase 8)
    status         VARCHAR(16) NOT NULL,
    last_event_seq BIGINT NOT NULL DEFAULT 0,          -- đã projection tới global_seq nào
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
