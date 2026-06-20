# ADR-0019: Đa tiền tệ + quy đổi tỉ giá (FX) — Phase 9

## Trạng thái
Accepted

## Bối cảnh
Mục Phase 9 "đa tiền tệ + quy đổi tỉ giá". Khác các mục Phase 9 còn lại (Kafka, microservice,
saga — cần hạ tầng), đây là **nghiệp vụ thuần backend, verify được bằng TDD**. Thách thức: giữ
invariant trung tâm "sổ luôn cân" khi tiền tồn tại ở nhiều tiền tệ.

## Quyết định

### 1. Tài khoản có tiền tệ; mỗi tiền tệ một vault
- `AccountOpened` mang thêm `currency` (mặc định VND; event/snapshot cũ null -> coi như VND, tương
  thích ngược). Mở tài khoản chỉ ở tiền tệ **đã cấu hình** (`ledger.vault.currencies`) — tránh tài
  khoản "mồ côi" không có vault để nạp/rút.
- **Mỗi tiền tệ một vault** khai sinh `seedAmount`: `SystemAccounts.vaultFor(ccy)` → VND giữ id cũ
  `"SYSTEM_VAULT"` (tương thích ngược), khác → `"SYSTEM_VAULT:<CCY>"`. Nạp/rút/lãi/capture dùng vault
  ĐÚNG TIỀN TỆ của tài khoản.

### 2. Chuyển tiền cùng tiền tệ; khác tiền tệ phải qua FX
- `transfer` yêu cầu hai tài khoản cùng tiền tệ (khác → lỗi rõ ràng, gợi ý dùng FX).
- **FX = HAI bút toán ghi sổ kép CÙNG TIỀN TỆ** bắc cầu qua vault: `nguồn(A) → vault(A)` cho `amount`
  và `vault(B) → đích(B)` cho `amount × tỉ giá`. Nhờ vậy **tổng theo TỪNG tiền tệ không đổi**
  (integrity per-currency vẫn cân); hệ thống đóng vai "quầy FX" (vault được phép âm).
- **Tỉ giá do hệ thống cấu hình** (`ledger.fx.rates.<FROM>.<TO>`), KHÔNG để client tự đặt (tránh tự
  "in tiền"). Idempotency-Key + ownership cả hai tài khoản.

### 3. Integrity theo từng tiền tệ
- `IntegrityService` đổi từ "tổng toàn cục == seed" sang "với MỖI tiền tệ, tổng == seedAmount";
  balanced=true chỉ khi mọi tiền tệ đều cân. VND-only (test mặc định) cho kết quả y như cũ.

### 4. Đúng đắn dưới làm tròn (sửa sau /code-review)
- Read model là `NUMERIC(20,2)`. `toAmount = amount × rate` có thể >2 chữ số → event store lưu chính
  xác nhưng read model làm tròn lệch hai vế → integrity báo **lệch sổ giả**. Sửa: **làm tròn toAmount
  về 2 chữ số (HALF_UP)** trước khi post, khớp scale read model. Hệ quả: granularity tiền tệ của hệ
  thống là 2 chữ số (mọi số tiền nên ≤ 2 chữ số).

## Hệ quả
**Được:** sổ cái đa tiền tệ thực thụ, FX giữ integrity per-currency; tương thích ngược hoàn toàn
(85 test cũ xanh không đổi). **Mất / đánh đổi / giới hạn (ghi rõ):**
- Granularity 2 chữ số (read model NUMERIC(20,2)); FX làm tròn về đó.
- FX chưa subject hạn mức ngày / fraud (luồng riêng); một tỉ giá đơn (chưa spread bid/ask).
- FX (4 posting, 2 tiền tệ) chưa hỗ trợ reverse qua endpoint reversal thường (giả định 1 cặp nợ/có).
- **UI cho FX chưa làm** (chọn tiền tệ khi mở tài khoản + màn quy đổi) — để follow-up; backend xong & verify.

## Phương án đã cân nhắc
- **Ghi sổ kép trực tiếp xuyên tiền tệ** (nợ A, có B khác số tiền) — loại: phá invariant "hai vế cùng
  số tiền"; bắc cầu qua vault giữ mỗi vế cân.
- **Client tự gửi tỉ giá** — loại: cho phép tự định giá = tự in tiền; tỉ giá phải do hệ thống.
- **Integrity toàn cục cộng mọi tiền tệ** — loại: cộng số dư khác tiền tệ là vô nghĩa; phải per-currency.
