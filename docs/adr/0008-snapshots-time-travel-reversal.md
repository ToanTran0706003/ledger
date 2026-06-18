# ADR-0008: Snapshot, time-travel và reversal

## Trạng thái
Accepted

## Bối cảnh
Phase 4 khai thác sức mạnh độc nhất của Event Sourcing và giải quyết hiệu năng load:
- Aggregate có lịch sử dài thì replay từ đầu mỗi lần load sẽ chậm dần.
- Cần trả lời "số dư tại thời điểm X" (time-travel) và "hủy hiệu lực một giao dịch"
  (reversal) mà không phá nguyên tắc append-only.

## Quyết định
**Snapshot:**
- Bảng `snapshots` (ghi đè theo aggregate_id). Sau mỗi **N event** trên một aggregate
  (cấu hình `ledger.snapshot.every-n-events`, mặc định 50) ghi snapshot trạng thái.
- Load = snapshot (nếu có) → khôi phục trạng thái + version → replay chỉ các event SAU
  version snapshot (`EventStore.loadStreamAfter`). Số event phải replay luôn ≤ N.
- `AccountRepository` là nơi tập trung load/append + ghi snapshot, để command handler
  không bận tâm. Snapshot là *tối ưu*: xóa đi load vẫn ra kết quả y hệt (có test).

**Time-travel:**
- `EventStore.loadStreamUntil(id, asOf)` lấy event có `occurred_at <= asOf`, replay →
  số dư tại thời điểm đó. Endpoint `GET /accounts/{id}/balance?asOf=<ISO-8601>`.

**Reversal:**
- Không xóa/sửa event. `ReverseTransactionHandler` tìm các posting của txId gốc (truy
  vấn events theo `payload->>'txId'`), tạo posting **ngược lại** (credit↔debit) cùng một
  txId mới, `movementType=REVERSAL`, `reversalOfTxId` trỏ về txId gốc. Hai vế ghi atomic.
- Nếu vế debit khi đảo làm tài khoản CUSTOMER âm (tiền đã tiêu) → invariant từ chối,
  reversal thất bại (đúng đắn). Không cho đảo bút toán GENESIS.

**Audit metadata:**
- `CorrelationFilter` gán `correlationId` (header `X-Correlation-Id` hoặc tự sinh) cho mỗi
  request, đặt vào `CorrelationContext` (ThreadLocal) + MDC. `JdbcEventStore.append` ghi
  `metadata` JSONB (correlationId) lên mỗi event. userId/ip sẽ thêm ở Phase 5 (auth).

## Hệ quả
**Được:**
- Load ≈ hằng số bất kể lịch sử dài (snapshot). Trả lời được "số dư ngày X" (time-travel).
- Sửa sai bằng bù trừ, giữ toàn vẹn lịch sử (reversal). Truy vết qua correlationId.

**Mất / đánh đổi:**
- `MoneyPosted` thêm field `reversalOfTxId` (additive, tương thích ngược — event cũ đọc ra null).
- Snapshot phải dọn/đồng bộ khi rebuild (rebuild đánh dấu outbox SENT; snapshot vẫn hợp lệ vì
  phản ánh event store). Test phải truncate cả `snapshots`.
- Reversal đọc events theo JSON `payload->>'txId'` — chấp nhận được ở quy mô hiện tại; có thể
  thêm index trên biểu thức nếu cần (Phase 6).
- Time-travel hiện replay từ đầu tới asOf (chưa dùng snapshot ≤ asOf) — tối ưu sau nếu cần.

## Phương án đã cân nhắc
- **Không snapshot, luôn replay từ đầu** — loại: load chậm tuyến tính theo lịch sử.
- **Xóa/sửa event khi reversal** — loại: vi phạm append-only (ADR-0002), mất audit.
- **Lưu reason reversal thành event riêng `TransactionReversed`** — hoãn: mô hình posting
  account-centric (ADR-0005) đã đủ; reason có thể đưa vào metadata sau.
