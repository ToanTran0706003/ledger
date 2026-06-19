-- Lộ loại tài khoản ra read model để UI phân biệt Thanh toán / Tiết kiệm.
-- Tài khoản cũ mặc định CUSTOMER; projector sẽ điền đúng cho tài khoản mới (và khi rebuild).
ALTER TABLE rm_account_balance ADD COLUMN account_type VARCHAR(16) NOT NULL DEFAULT 'CUSTOMER';
