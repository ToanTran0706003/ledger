-- Snapshot: ảnh chụp trạng thái aggregate mỗi N event để tăng tốc load.
-- Đây là tối ưu (không phải nguồn sự thật) -> ghi đè, có thể xóa và dựng lại từ event store.
CREATE TABLE snapshots (
    aggregate_id      VARCHAR(64) PRIMARY KEY,
    aggregate_type    VARCHAR(64) NOT NULL,
    aggregate_version INT NOT NULL,        -- snapshot tính đến version này
    state             JSONB NOT NULL,      -- trạng thái đã serialize
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
