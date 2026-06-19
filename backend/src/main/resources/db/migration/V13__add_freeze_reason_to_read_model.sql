-- Hỗ trợ đóng băng tài khoản (kiểm soát gian lận): lưu lý do đóng băng để hiển thị/kiểm toán.
-- status (ACTIVE/FROZEN) đã có sẵn trong rm_account_balance từ V2.
ALTER TABLE rm_account_balance ADD COLUMN freeze_reason VARCHAR(255);
