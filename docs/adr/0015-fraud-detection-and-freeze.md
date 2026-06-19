# ADR-0015: Phát hiện gian lận rule-based + đóng băng tài khoản

## Trạng thái
Accepted

## Bối cảnh
Giám sát giao dịch và đóng băng tài khoản nghi ngờ là năng lực kiểm soát rủi ro cốt lõi của
ngân hàng/fintech. Cần: (1) một trạng thái "đóng băng" chặn tiền chảy ra, (2) luật tự phát hiện
hành vi bất thường và đóng băng, (3) công cụ cho ADMIN xử lý thủ công.

## Quyết định

### 1. Đóng băng là năng lực của domain (event-sourced)
- Event `AccountFrozen(reason)` / `AccountUnfrozen`; trạng thái `FROZEN` (đã có sẵn trong enum).
- **Invariant:** tài khoản FROZEN **không được ghi nợ** (chặn thất thoát) nhưng **vẫn nhận ghi
  có** (deposit/chuyển đến/lãi). Kiểm tra trong `AccountAggregate.debit` trước cả kiểm tra số dư.
- Trạng thái nằm trong snapshot nên freeze sống sót qua snapshot/replay. Event đi qua append nên
  được hash-chain bảo vệ (ADR-0014).

### 2. Luật phát hiện gian lận (best-effort, hậu kỳ)
- `FraudService.evaluate(accountId, amount)` chạy **sau khi** lệnh ghi nợ đã commit (withdraw/
  transfer). Hai luật đơn giản: **giao dịch lớn bất thường** (số tiền lệnh ≥ ngưỡng) và **tần
  suất cao** (số lệnh WITHDRAWAL/TRANSFER trong cửa sổ ≥ ngưỡng). Chạm luật → tự đóng băng kèm lý do.
- Chỉ đếm `WITHDRAWAL`/`TRANSFER` (loại REVERSAL/hệ thống) để admin reverse không vô tình đóng băng khách.
- **Best-effort, không chặn giao dịch gốc:** evaluate là side-effect *ngoài* transaction gốc; lỗi
  khi đóng băng (đua đồng thời → `AccountStateConflictException`, hoặc lỗi DB) được **nuốt + log**,
  KHÔNG làm hỏng giao dịch đã thành công (tiền đã đi, txId đã có). Đây là phát hiện *hậu kỳ* (ngăn
  thất thoát TIẾP THEO), đúng mô hình giám sát giao dịch thực tế.
- Tắt được qua `ledger.fraud.enabled` (mặc định bật; **tắt trong test** để không nhiễu kịch bản khác,
  test fraud tự bật + đặt ngưỡng thấp qua `@TestPropertySource`). Ngưỡng cấu hình bằng `@Value`
  (đồng nhất với cách savings/snapshot cấu hình).

### 3. Công cụ ADMIN
- `POST /admin/accounts/{id}/freeze` (lý do), `/unfreeze`, `GET /admin/fraud/frozen` (danh sách).
- Read model `rm_account_balance` thêm cột `freeze_reason` (V13); projector cập nhật status + lý do.
- `AccountStateConflictException extends IllegalStateException` → map **409**; tách riêng để không
  nuốt các `IllegalStateException` do lỗi hạ tầng (serialize snapshot, event lạ) thành 409.

## Hệ quả
**Được:** kiểm soát rủi ro chạy end-to-end; đóng băng an toàn (chỉ chặn ghi nợ); luật tái dùng
read model sẵn có; chi phí 0–1 query/lệnh ghi nợ (chỉ query đếm khi không phải giao dịch lớn).
**Mất / đánh đổi / giới hạn (ghi rõ, không overclaim):**
- Luật là **heuristic** + đọc read model **eventually-consistent** (qua outbox): dưới tải đồng thời
  cao, số đếm tần suất có thể trễ một nhịp — phát hiện best-effort, không đảm bảo realtime.
- Phát hiện **hậu kỳ**: giao dịch kích hoạt vẫn thành công; chỉ chặn các lệnh sau. Maker-checker
  (chặn trước khi vượt ngưỡng) là hướng khác, chưa làm.
- `FraudService.mutate` lặp lại khuôn executor+drain của `HoldService`/`MoneyMovementHandler` —
  cố ý không gộp lúc này (gộp sẽ phải sửa cả hai, không surgical); ghi nợ kỹ thuật.

## Phương án đã cân nhắc
- **Listener phản ứng sự kiện `MoneyPosted`** (thay vì gọi trong command handler) — hoãn: phản ứng
  trong lúc projection/drain dễ tái nhập (raise event mới khi đang drain); gọi hậu kỳ trong handler
  đơn giản và đủ. Vì đã nuốt lỗi nên không ảnh hưởng giao dịch gốc.
- **Đóng băng chặn cả ghi có** — loại: tiền vào tài khoản nghi ngờ vẫn nên ghi nhận; chỉ chặn ra.
- **Map IllegalStateException → 409 chung** — loại: nuốt mất lỗi hạ tầng (đáng lẽ 500); dùng exception riêng.
