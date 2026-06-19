# ADR-0012: Nghiệp vụ nâng cao — tiết kiệm/lãi & lệnh định kỳ

## Trạng thái
Accepted

## Bối cảnh
Phase 8 làm "dày" nghiệp vụ với ít nhất hai sản phẩm chạy end-to-end, tận dụng đúng
sức mạnh Event Sourcing. Phát triển theo **TDD** (viết test trước cho phần lõi) và
kết thúc bằng một lượt **code-review**.

## Quyết định

### 1. Tài khoản tiết kiệm + tính lãi qua replay
- `InterestCalculator` (hàm thuần, TDD red→green): lãi đơn theo **số dư bình quân gia
  quyền thời gian** — Σ(số dư × thời lượng) × lãi suất / năm. Dùng **nano giây** (không
  `getSeconds()`) để không mất phần dưới giây (lỗi do code-review/test phát hiện).
- `InterestService` dựng các đoạn (số dư, thời lượng) bằng cách **replay** event của tài
  khoản tới mốc `asOf`, rồi áp `InterestCalculator`. Lãi trả **từ SYSTEM_VAULT** sang tài
  khoản (ghi sổ kép) nên tổng tiền hệ thống vẫn cân (integrity).
- **Tính + ghi trong CÙNG một transaction** (executor): nếu hai lần accrue chạy đồng thời,
  cả hai đụng vault → một bên ConcurrencyConflict → retry tính lại `from` (đã thấy posting
  INTEREST vừa ghi) → lãi = 0. Nhờ vậy không trả lãi hai lần cho một kỳ.
- `asOf` bị **chặn không vượt quá hiện tại** (không tính lãi cho thời gian chưa trôi).
- Endpoint `POST /admin/accounts/{id}/accrue-interest` (ADMIN — ngân hàng trả lãi).

### 2. Lệnh chuyển tiền định kỳ (standing order)
- `StandingOrder` (JPA — trạng thái CRUD) + scheduler `@Scheduled` (tắt được, tắt trong test).
- `runDue`: **dời chu kỳ và lưu TRƯỚC khi chuyển** → "at-most-once": vì chuyển tiền không
  idempotent (mỗi lần txId mới), nếu crash sau khi chuyển thì lệnh đã được dời nên không
  chạy lại (không trừ tiền hai lần); mất một kỳ khi crash an toàn hơn trừ trùng.
- Validate **tài khoản nhận tồn tại** lúc tạo (tránh lệnh "chết âm thầm"). Chỉ chủ tài khoản
  nguồn được tạo lệnh (ownership). Endpoint `POST/GET /standing-orders`.

## Hệ quả
**Được:** hai sản phẩm nâng cao chạy end-to-end; tính lãi minh hoạ trọn vẹn ES (replay +
time-weighting); standing order tái dùng scheduler/outbox/money-movement.
**Mất / đánh đổi:**
- Lãi: phần tính + ghi gắn chặt trong một transaction (cố ý, để retry tự sửa — không tách
  reuse `MoneyMovementHandler` vì sẽ phá tính chất idempotent này).
- Standing order at-most-once: có thể bỏ một kỳ khi sự cố (chấp nhận, an toàn hơn trùng).
- Chưa làm: hold/reservation, fraud detection, hạn mức ngày (🟢). UI chưa surface (API sẵn).

## Phương án đã cân nhắc
- **Lãi tính ngoài transaction rồi mới post** — loại: hai accrue đồng thời sẽ trả lãi đôi
  (retry chỉ post lại cùng số tiền, không tính lại).
- **Standing order "at-least-once" (chuyển trước, dời sau)** — loại: chuyển không idempotent
  → crash giữa chừng gây trừ tiền hai lần.
- **Hold/reservation cho sản phẩm thứ hai** — hoãn: đụng vào invariant lõi (available vs
  balance), rủi ro cao hơn; standing order gọn và tái dùng hạ tầng sẵn có.
