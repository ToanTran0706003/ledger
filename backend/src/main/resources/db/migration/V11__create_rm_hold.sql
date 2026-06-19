-- Read model: các hold (giữ tiền) trên tài khoản. Phái sinh từ event (HoldPlaced/HoldReleased),
-- có thể dựng lại từ event store. Dùng để liệt kê hold và cho scheduler tìm hold hết hạn.
CREATE TABLE rm_hold (
    hold_id     VARCHAR(64) PRIMARY KEY,
    account_id  VARCHAR(64) NOT NULL,
    amount      NUMERIC(20,2) NOT NULL,
    status      VARCHAR(16) NOT NULL,          -- ACTIVE | RELEASED | CAPTURED
    reason      VARCHAR(16),                   -- lý do nhả: MANUAL | EXPIRED | CAPTURED (null khi còn ACTIVE)
    placed_at   TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ
);

CREATE INDEX idx_rm_hold_account ON rm_hold (account_id);
-- Scheduler quét hold đến hạn: lọc theo trạng thái ACTIVE và mốc hết hạn.
CREATE INDEX idx_rm_hold_due ON rm_hold (status, expires_at);
