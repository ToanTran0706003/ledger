# ADR-0016: Hạn mức giao dịch theo ngày

## Trạng thái
Accepted

## Bối cảnh
Hạn mức rút/chuyển theo ngày là kiểm soát rủi ro chuẩn của ngân hàng, hoàn thiện bộ ba cùng
hold (giữ chỗ) và fraud detection (đóng băng). Khác fraud (phát hiện hậu kỳ, best-effort),
hạn mức là **ràng buộc cứng**: lệnh làm vượt hạn mức bị **từ chối ngay**, không thực hiện.

Điểm khó kỹ thuật: ràng buộc theo **cửa sổ thời gian (1 ngày)** trong Event Sourcing. Nếu nhồi
trạng thái này vào aggregate thì snapshot phình (phải giữ danh sách posting theo thời gian);
nếu đọc read model thì eventually-consistent (có thể vượt nhẹ khi đồng thời).

## Quyết định
- **Kiểm tra ngay TRONG transaction di chuyển tiền** (`MoneyMovementHandler.move`, trước vế
  ghi nợ): cộng tổng ghi nợ trong ngày của tài khoản từ **event store** (nguồn sự thật, đã
  commit) bằng một truy vấn `SUM((payload->>'amount')::numeric)` lọc `direction='DEBIT'` và
  `movementType IN (WITHDRAWAL, TRANSFER)`. Nếu `tổng_hôm_nay + amount > hạn_mức` → ném
  `DailyLimitExceededException` (HTTP 422).
- **Chính xác cả khi đồng thời** (không chỉ best-effort): hai lệnh ghi nợ cùng một tài khoản
  đều tăng version của *cùng* aggregate → đụng `UNIQUE(aggregate_id, aggregate_version)` →
  một bên `ConcurrencyConflict` → `RetryingTransactionExecutor` thử lại, **load lại version
  mới và kiểm tra lại hạn mức** (đã thấy lệnh kia). Optimistic concurrency sẵn có (ADR-0006)
  serialize ghi nợ theo tài khoản nên tổng đếm luôn nhất quán.
- **Ranh giới đầu ngày tính bằng đồng hồ DB** trong múi giờ cấu hình:
  `occurred_at >= date_trunc('day', now() AT TIME ZONE :zone) AT TIME ZONE :zone`. Cùng nguồn
  đồng hồ với `occurred_at` (DB `now()`), nên không lệch do drift JVM/DB.
- **Phạm vi:** chỉ áp cho tài khoản khách (vault là nguồn/đích hệ thống — miễn). Nạp tiền
  (vault là nguồn) không tính. Reversal/capture/interest đi qua handler khác và bị lọc khỏi
  tổng (chỉ đếm WITHDRAWAL/TRANSFER do khách khởi tạo).
- Tắt được qua `ledger.daily-limit.enabled` (mặc định bật; **tắt trong test**, test hạn mức
  tự bật + ngưỡng thấp qua `@TestPropertySource`). Ngưỡng + múi giờ cấu hình bằng `@Value`.

## Hệ quả
**Được:** hạn mức **cứng & chính xác** (mạnh hơn best-effort), **không cần migration** (tái dùng
bảng events + index `idx_events_aggregate`), aggregate vẫn gọn (không nhồi trạng thái cửa sổ).
Hoàn tất bộ kiểm soát rủi ro Phase 8 (hold + fraud + daily-limit).
**Mất / đánh đổi:** thêm một truy vấn (có index theo aggregate_id) mỗi lệnh rút/chuyển của khách.
Capture (thu hold) hiện không tính vào hạn mức (đi đường riêng) — chấp nhận, có thể mở rộng sau.

## Phương án đã cân nhắc
- **Đọc read model `rm_transaction_history`** — loại: eventually-consistent (cập nhật sau commit
  qua drain) nên có thể vượt nhẹ khi đồng thời; đọc event store đã commit + serialize theo
  aggregate cho kết quả chính xác.
- **Nhồi trạng thái cửa sổ vào aggregate** — loại: phải giữ danh sách posting theo thời gian →
  snapshot phình, trái với chủ trương aggregate gọn (chỉ giữ số dư/holds/status).
- **Tính ranh giới ngày ở JVM** (bản nháp đầu, /code-review phát hiện) — đổi: lệch do drift
  JVM/DB ở sát nửa đêm; chuyển sang tính bằng đồng hồ DB.
