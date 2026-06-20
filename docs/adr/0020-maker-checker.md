# ADR-0020: Maker-checker (nguyên tắc bốn-mắt) cho giao dịch vượt ngưỡng

## Trạng thái
Accepted

## Bối cảnh
Phân tách nhiệm vụ (segregation of duties) là kiểm soát chuẩn của ngân hàng: giao dịch lớn phải
được một người DUYỆT KHÁC người tạo phê duyệt. Hoàn thiện bộ kiểm soát rủi ro của dự án (hold +
fraud-freeze + hạn mức ngày + maker-checker).

## Quyết định
- **Chuyển tiền ≥ ngưỡng → tạo yêu cầu chờ duyệt** (`PendingTransfer`) thay vì thực thi ngay; dưới
  ngưỡng chạy bình thường. `PendingTransfer` là **trạng thái CRUD (JPA)**, KHÔNG event-sourced —
  chỉ khi được DUYỆT mới phát giao dịch thật vào event store (giống standing order, ADR-0012).
- **Nguyên tắc bốn-mắt:** người duyệt phải KHÁC người tạo (`makerUserId != approverUserId`). Tự
  duyệt → 403. Từ chối → không thực thi. Lưu cả maker và checker (+ thời điểm, lý do) để kiểm toán.
- **Validation ở service** (đang chờ + bốn-mắt) TRƯỚC khi thực thi (tránh "chuyển xong rồi mới
  báo lỗi"); entity chỉ giữ trạng thái.
- **HTTP:** `POST /transfers` trả `EXECUTED` (201) hoặc `PENDING_APPROVAL` (202).
  `GET/POST /admin/approvals[/{id}/approve|reject]` (ADMIN). UI: màn chuyển tiền báo "chờ duyệt";
  màn **Quản trị** thêm mục "Giao dịch chờ duyệt" (Duyệt/Từ chối).
- Cấu hình `ledger.maker-checker.{enabled, threshold}` (mặc định bật, ngưỡng 100tr; tắt trong test,
  test bật + ngưỡng thấp qua `@TestPropertySource`).

## Hệ quả
**Được:** kiểm soát phân tách nhiệm vụ chạy end-to-end (backend + UI); khép kín bộ kiểm soát rủi ro.
Tái dùng `MoneyMovementHandler.transfer` khi duyệt (giữ ghi sổ kép + idempotency + retry).
**Mất / đánh đổi / giới hạn:**
- Chỉ áp cho **transfer** (withdraw/FX có thể mở rộng tương tự).
- Một ngưỡng duy nhất, không phân theo tiền tệ.
- Đồng thời: hai lần duyệt song song cùng một yêu cầu có thể thực thi đôi (cửa sổ validate→execute) —
  ghi nợ kỹ thuật; hardening tương lai: optimistic locking (`@Version`) trên `PendingTransfer`.

## Phương án đã cân nhắc
- **Event-source yêu cầu duyệt** — loại: đây là trạng thái quy trình, chưa phải di chuyển tiền cho
  tới khi duyệt; JPA (như standing order/users) phù hợp hơn.
- **Thực thi rồi mới đánh dấu duyệt** — loại: nếu đánh dấu lỗi sau khi đã chuyển sẽ lệch; validate
  trước khi thực thi an toàn hơn.
- **Áp maker-checker cho mọi lệnh ghi nợ** — thu hẹp về transfer cho gọn; mở rộng sau dễ.
