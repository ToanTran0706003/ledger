# ADR-0007: Idempotency-Key cho các endpoint ghi tiền

## Trạng thái
Accepted

## Bối cảnh
Mạng không tin cậy: client gửi "chuyển 500k", timeout, rồi retry → server có thể nhận
hai lần → trừ tiền hai lần. Optimistic concurrency (ADR-0006) chống *các lệnh khác nhau*
đè nhau, nhưng KHÔNG chống *cùng một lệnh lặp lại* — đó là việc của idempotency.

## Quyết định
Mỗi endpoint ghi tiền (deposit/withdraw/transfer) yêu cầu header **`Idempotency-Key`**.
`IdempotencyService` (bảng `idempotency_keys`):
- **Key mới:** ghi `IN_PROGRESS` (INSERT, PK đảm bảo chỉ một bên giành được), chạy action,
  lưu response + `COMPLETED`.
- **Key đã COMPLETED:** trả lại response cũ, KHÔNG chạy lại.
- **Key đang IN_PROGRESS:** 409 (đang xử lý) — chặn chạy song song trùng.
- **Cùng key, payload khác** (`request_hash` lệch): 422.
- **Action thất bại** (vd không đủ số dư): nhả key (xóa IN_PROGRESS) để client thử lại được.

Các thao tác idempotency chạy ngoài transaction nghiệp vụ (autocommit) nên dòng IN_PROGRESS
hiện ngay với request trùng; action tự quản transaction riêng (qua executor ADR-0006).

## Hệ quả
**Được:**
- Gửi trùng cùng key → đúng một hiệu lực, lần sau trả lại đúng kết quả cũ (có test).
- Hai lớp phòng thủ độc lập: idempotency (lệnh lặp) + optimistic concurrency (lệnh đè nhau).

**Mất / đánh đổi:**
- Client phải sinh và gửi `Idempotency-Key` (UUID) cho mỗi thao tác ghi; thiếu header → 400.
- Bảng `idempotency_keys` cần dọn dẹp theo `expires_at` (TTL) — để dành về sau.
- Lưu `response_body` dạng JSON để replay; ràng buộc response phải serialize được.
- Hiện áp dụng cho 3 endpoint tiền; mở tài khoản chưa bắt buộc key (rủi ro thấp).

## Phương án đã cân nhắc
- **Không idempotency, chỉ dựa concurrency** — loại: không chống được lệnh lặp y hệt
  (cùng nội dung, gửi 2 lần) → trừ tiền 2 lần.
- **Khử trùng bằng client request-id lưu trong aggregate** — loại: trộn mối quan tâm hạ
  tầng vào domain; bảng idempotency tách bạch và tái dùng cho mọi endpoint.
- **Filter/Interceptor cache response tự động** — hoãn: cần wrap response phức tạp; cách
  service tường minh ở controller dễ kiểm thử hơn cho giai đoạn này.
