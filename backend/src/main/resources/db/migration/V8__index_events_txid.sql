-- Tối ưu tra cứu posting theo txId (dùng khi reversal). Index trên biểu thức JSON,
-- chỉ cho event MoneyPosted (xem ADR-0010). Trước đó là tuần tự quét bảng events.
CREATE INDEX idx_events_txid ON events ((payload->>'txId')) WHERE event_type = 'MoneyPosted';
