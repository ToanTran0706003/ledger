# 04 — Security & Anti-Cheat

> Đây là chương quan trọng nhất về mặt giá trị kỹ thuật. Trong domain tài chính, "chống cheat" không phải chống người chơi gian — mà là **đảm bảo tính đúng đắn của tiền dưới mọi điều kiện**: chạy đồng thời, lỗi mạng, tấn công, lỗi lập trình.

## 1. Phân loại mối đe dọa (Threat Model)

| Loại | Mô tả | Đối sách |
|------|-------|----------|
| Race condition | 2 lệnh đồng thời trên cùng tài khoản → số dư sai | Optimistic concurrency (version) |
| Double-spending | Rút/chuyển nhiều hơn số dư bằng cách lợi dụng đồng thời | Aggregate invariant + concurrency |
| Replay request | Cùng 1 lệnh gửi nhiều lần (mạng lag, retry) | Idempotency key |
| Lệch sổ (tiền bốc hơi/tự sinh) | Bug khiến tổng tiền sai | Double-entry + integrity check |
| Truy cập trái phép | User A đọc/ghi tài khoản user B | AuthZ chặt, kiểm tra ownership |
| Giả mạo danh tính | Mạo danh user khác | JWT ký, xác thực chặt |
| Tampering dữ liệu | Sửa event trong DB | Append-only + hash chain (tùy chọn) |
| Gian lận hành vi | Rửa tiền, giao dịch bất thường | Fraud detection trên chuỗi event |
| Injection / input độc | SQLi, payload độc | Validation, prepared statements (JPA) |

## 2. Authentication (xác thực)

- **JWT** access token ngắn hạn + refresh token. Token ký bằng khóa bí mật/cặp khóa.
- Mật khẩu băm bằng **BCrypt/Argon2** (không bao giờ lưu plaintext).
- Rate limit đăng nhập chống brute-force.
- (Nâng cao) hỗ trợ 2FA/TOTP cho thao tác nhạy cảm.

> **Lưu ý phạm vi:** Dự án không tự xử lý thông tin thẻ/tài khoản ngân hàng thật. Mọi "tiền" là nội bộ.

## 3. Authorization (phân quyền)

- **Ownership check bắt buộc:** mọi truy vấn/lệnh trên `accountId` phải xác minh tài khoản thuộc user đang đăng nhập (hoặc user có vai trò admin hợp lệ).
- **Vai trò:** CUSTOMER, ADMIN, AUDITOR (chỉ đọc audit), SYSTEM.
- **Nguyên tắc đặc quyền tối thiểu:** mỗi vai trò chỉ thấy/làm đúng phần của mình.
- (Nâng cao) tài khoản doanh nghiệp cần **maker-checker**: giao dịch lớn cần người thứ hai duyệt → sinh event riêng cho mỗi bước.

## 4. Optimistic Concurrency Control — chống race & double-spend

### Vấn đề minh họa
Tài khoản 1.000.000₫. Hai request đồng thời cùng rút 800.000₫:
- Cả hai đọc số dư = 1.000.000 (đủ).
- Cả hai cho phép rút.
- Kết quả: −600.000₫. **Tiền từ trên trời.**

### Cơ chế bảo vệ
1. Load aggregate → biết `version = v`.
2. Validate invariant trên trạng thái đã load.
3. Append event với `expectedVersion = v` (DB có `UNIQUE(aggregate_id, aggregate_version)`).
4. Hai request cùng cố ghi `version = v+1` → DB chỉ chấp nhận một, cái còn lại vi phạm unique constraint.
5. Request thua nhận `ConcurrencyConflict` → **retry** (đọc lại version mới, validate lại). Lần này số dư đã giảm → rút lần hai bị từ chối đúng đắn.

```
Request A: load v7 → validate OK → append v8 ✓
Request B: load v7 → validate OK → append v8 ✗ (conflict)
           → retry: load v8 (balance đã giảm) → validate FAIL → từ chối
```

→ **Không bao giờ tạo số dư âm sai.** Đây là điểm phỏng vấn cực mạnh.

## 5. Idempotency — chống replay/double-execution

### Vấn đề
Client gửi "chuyển 500k", mạng lag, client retry → server nhận 2 lần → trừ 1 triệu.

### Cơ chế
- Client gắn header `Idempotency-Key` (UUID) cho mỗi thao tác ghi.
- Server:
  1. Tra `idempotency_keys`. Nếu key đã `COMPLETED` → trả lại response cũ, **không chạy lại**.
  2. Nếu `IN_PROGRESS` → trả 409 (đang xử lý) để tránh chạy song song.
  3. Nếu mới → ghi `IN_PROGRESS`, xử lý, lưu kết quả, đánh dấu `COMPLETED`.
- `request_hash` phát hiện trường hợp dùng lại key cho payload khác (lỗi client) → từ chối.

> Idempotency + concurrency là hai lớp phòng thủ *khác nhau*: idempotency chống *cùng một lệnh lặp lại*; concurrency chống *các lệnh khác nhau chạy đè nhau*.

## 6. Tính toàn vẹn sổ cái (Ledger Integrity) — tính năng "wao"

### Invariant toàn cục
Nhờ double-entry, với mọi thời điểm:
```
SUM(balance của mọi account, bao gồm SYSTEM_VAULT) == hằng số khởi tạo
```
Mỗi transaction cân vế (Σ debit = Σ credit) nên tổng không bao giờ đổi.

### Cơ chế kiểm tra
- Endpoint `/audit/integrity`: tính tổng tất cả số dư, so với hằng số. Lệch → cảnh báo đỏ.
- **Reconciliation:** đối chiếu read model với kết quả replay từ event store. Lệch → projector có bug.
- **Property-based test:** sinh hàng nghìn giao dịch ngẫu nhiên (deposit/withdraw/transfer hợp lệ và không hợp lệ), sau mỗi bước assert: không có số dư CUSTOMER âm, tổng hệ thống không đổi, mọi transaction cân vế.

> Có được ba thứ trên là dấu hiệu rõ ràng của một kỹ sư hiểu hệ thống tài chính. Rất hiếm trong portfolio.

## 7. Bất biến & chống tampering event store
- Không có API nào UPDATE/DELETE bảng `events`. Quyền DB cấp đúng mức (chỉ INSERT/SELECT).
- (Nâng cao) **Hash chain:** mỗi event lưu hash của event trước đó → tạo chuỗi không thể sửa giữa chừng mà không bị phát hiện (kiểu blockchain nhẹ). Một bước kiểm tra quét toàn chuỗi xác nhận chưa ai chỉnh sửa.
- Audit metadata: mỗi event ghi `userId`, `ip`, `correlationId`, `occurredAt` → truy vết đầy đủ.

## 8. Fraud Detection (cơ bản → nâng cao)
- **Rule-based (cơ bản):** ngưỡng giao dịch/ngày, số lần thất bại, giao dịch lớn bất thường so với lịch sử → sinh event `FraudAlertRaised`, có thể tự động FREEZE.
- **Tận dụng Event Sourcing:** vì có sẵn toàn bộ lịch sử hành vi, việc phân tích pattern (velocity, structuring) trở nên tự nhiên.
- **Nâng cao (tùy chọn):** scoring theo đặc trưng hành vi; không cần ML phức tạp, rule tốt đã ấn tượng.

## 9. Bảo mật ứng dụng (application security)
- Validation chặt mọi input (Bean Validation), từ chối sớm.
- Dùng JPA/prepared statements → tránh SQL injection.
- Không lộ thông tin nhạy cảm trong log/lỗi (không log token, không stacktrace ra client).
- CORS cấu hình đúng, security headers (CSP, HSTS).
- Secrets qua biến môi trường, không hardcode, không commit.
- Dependency scanning (OWASP Dependency-Check) trong CI.

## 10. Quyền riêng tư & dữ liệu
- Tối thiểu hóa PII; nếu có, mã hóa khi nghỉ (at-rest) cho field nhạy cảm.
- Không đặt dữ liệu nhạy cảm trong URL/query string.
- Phân tách rõ dữ liệu giữa các user (ownership check) — không bao giờ rò rỉ chéo.

## 11. Checklist bảo mật (rút gọn để review)
- [ ] Mọi endpoint ghi yêu cầu Idempotency-Key
- [ ] Mọi truy cập accountId qua ownership check
- [ ] Optimistic concurrency bật trên mọi aggregate
- [ ] Integrity check chạy được và pass trên property test
- [ ] Mật khẩu băm Argon2/BCrypt
- [ ] JWT ký & verify đúng, refresh token an toàn
- [ ] events không có path UPDATE/DELETE
- [ ] Secrets không nằm trong repo
- [ ] Dependency scan sạch trong CI

## 12. Bước kế tiếp
Đọc `05-performance-and-scaling.md`.
