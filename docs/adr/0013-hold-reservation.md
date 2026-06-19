# ADR-0013: Hold/Reservation — số dư khả dụng vs thực

## Trạng thái
Accepted

## Bối cảnh
ADR-0012 đã hoãn hold/reservation vì nó "đụng vào invariant lõi (available vs balance)".
Đây là sản phẩm nghiệp vụ kinh điển của fintech (ủy quyền thanh toán kiểu thẻ): giữ trước
một phần tiền, sau đó hoặc thu (capture) hoặc nhả (release), và tự nhả khi hết hạn. Nó là
phần "khó về kỹ thuật" đúng tinh thần dự án vì buộc phải tách **số dư khả dụng** khỏi **số
dư thực** và sửa lại bất biến chống-âm.

## Quyết định

### 1. Mô hình: `available = balance − Σ(hold đang giữ)`
- Hold là **trạng thái của aggregate** (không phải bảng JPA rời như standing order), vì nó
  ảnh hưởng trực tiếp tới invariant. Lưu dưới dạng map `holdId → amount` trong
  `AccountAggregate`, dựng lại bằng replay event `HoldPlaced`/`HoldReleased`.
- **Invariant lõi đổi:** `debit()` giờ chặn theo `available()` thay vì `balance`. Tài khoản
  chưa có hold thì `available == balance` nên nạp/rút/chuyển cũ **không đổi hành vi** (47
  test cũ vẫn xanh) — thay đổi an toàn, có kiểm chứng.
- **Snapshot mang theo map holds.** Nếu thiếu, khôi phục từ snapshot sẽ mất hold và làm vỡ
  invariant; có test round-trip riêng. Snapshot cũ (thiếu trường `holds`) được xử lý null-safe.

### 2. Vòng đời hold (event-sourced)
- `HoldPlaced(accountId, holdId, amount, placedAt, expiresAt)` — yêu cầu `available ≥ amount`.
- `HoldReleased(accountId, holdId, amount, reason, releasedAt)` với `reason ∈ {MANUAL,
  EXPIRED, CAPTURED}`. Event **tự mang theo `amount`** để projector cập nhật `available` mà
  không phải tra cứu hold gốc (event tự đủ thông tin).
- **Capture = nhả chỗ (CAPTURED) + ghi sổ kép.** Vế nợ tài khoản và vế có SYSTEM_VAULT do
  **tầng service** ghi (qua `debit`/`credit`), giữ **cả hai vế của một lần di chuyển tiền ở
  cùng một chỗ** — đối xứng với luồng chuyển tiền thường (`MoneyMovementHandler.move`).
  Aggregate chỉ chịu trách nhiệm phần riêng của hold (nhả chỗ). Thứ tự nhả-trước-debit-sau
  bảo đảm `debit` kiểm tra trên available đã khôi phục.
- **Capture chuyển tiền về SYSTEM_VAULT** ("thanh toán" ra ngoài hệ thống, một chiều như rút
  tiền). Đơn giản, đúng simplicity-first; hold kiểu transfer (capture sang tài khoản đích bất
  kỳ) để sau.

### 3. Hết hạn tự nhả
- `HoldExpiryScheduler` (@Scheduled, tắt được, tắt trong test) gọi `HoldService.expireDue`:
  quét read model `rm_hold` tìm hold `ACTIVE` đã quá hạn → `releaseHold(EXPIRED)`. Lỗi một
  hold (vd vừa bị nhả bởi luồng khác) không chặn các hold khác.

### 4. API & đọc
- `POST/GET /accounts/{id}/holds`, `POST .../{holdId}/release|capture`. Ownership check như
  các endpoint khác. **Place và capture** tác động tới tiền nên dùng `Idempotency-Key`
  (ADR-0007); release không đổi balance nên bỏ qua.
- Read model `rm_hold` (migration V11) để liệt kê và làm đầu vào cho scheduler; cột
  `available` đã có sẵn trong `rm_account_balance` từ V2.

## Hệ quả
**Được:** tách available/balance đúng chuẩn fintech; capture giữ integrity (tổng tiền không
đổi); tái dùng hạ tầng sẵn có (executor + outbox + snapshot + idempotency + scheduler).
**Mất / đánh đổi:**
- Capture chỉ về vault (chưa hỗ trợ hold-transfer sang tài khoản đích) — cố ý giữ gọn.
- Capture toàn phần (chưa partial capture / partial release) — đủ minh hoạ, mở rộng sau dễ.
- Scheduler lấy danh sách hold đến hạn từ read model (eventually consistent) → SLA hết hạn là
  "gần đúng theo chu kỳ poll", không tức thời. Chấp nhận được cho nghiệp vụ này.

## Phương án đã cân nhắc
- **Để debit vẫn chặn theo balance, available chỉ là chỉ số hiển thị** — loại: không thực sự
  ngăn tiêu vào phần đã giữ, mất ý nghĩa hold.
- **Hold là bảng JPA tách rời (như standing order)** — loại: hold ảnh hưởng invariant nên phải
  là trạng thái aggregate, không thể là read-side thuần.
- **Capture nhúng cả vế nợ vào trong aggregate** (bản nháp đầu, /code-review phát hiện) — đổi:
  tách vế nợ ra service để hai vế ghi sổ kép nằm cùng chỗ, dễ đọc, đồng nhất với `move`.
