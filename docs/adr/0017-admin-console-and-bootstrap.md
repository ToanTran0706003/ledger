# ADR-0017: Console Quản trị/Kiểm toán + tài khoản admin khởi tạo

## Trạng thái
Accepted

## Bối cảnh
Hai năng lực mạnh đã có ở API nhưng **chưa có UI**: xác minh hash-chain (ADR-0014, chứng minh
sổ không bị giả mạo — chỉ ADMIN/AUDITOR) và quản lý đóng băng gian lận (ADR-0015 — chỉ ADMIN).
Ngoài ra `register` chỉ tạo vai trò CUSTOMER, nên **không có tài khoản ADMIN nào** để dùng/demo
các tính năng theo vai trò.

## Quyết định
- **Màn "Quản trị" ở frontend** (hiện trong nav theo vai trò): xác minh hash-chain (ADMIN/AUDITOR)
  và danh sách tài khoản đóng băng + mở băng (ADMIN). Surface đúng các endpoint sẵn có.
- **Đọc vai trò ở client bằng cách giải mã claim `roles` của JWT** (không cần endpoint mới). Việc
  này CHỈ để bật/tắt affordance UI; **phân quyền thật vẫn do server thực thi** (`/admin/**`=ADMIN,
  `/audit/**`=ADMIN/AUDITOR — defense in depth). UI ẩn nút không thay thế authz backend.
- **Tài khoản admin khởi tạo (bootstrap)** seed lúc khởi động (idempotent, BCrypt như mọi user).
  - **Bật mặc định cho dev/demo** (đăng nhập ngay với `admin`), **TẮT trong profile prod**
    (`application-prod.yml`), mật khẩu lấy từ `LEDGER_ADMIN_PASSWORD` (mặc định dev có ghi tài liệu).
  - Khi tạo, **ghi log cảnh báo** đổi mật khẩu / tắt ngoài dev. An toàn-mặc-định ở prod.

## Hệ quả
**Được:** chuỗi giá trị audit/risk chạy **end-to-end tới UI** — kiểm toán viên bấm một nút để xác
minh sổ cái nguyên vẹn; quản trị viên xử lý tài khoản đóng băng. Demo thuyết phục. Không thêm
endpoint (tái dùng `roles` trong JWT).
**Mất / đánh đổi:** tài khoản admin mật khẩu mặc định là tiện ích dev — đã làm an toàn-mặc-định ở
prod (tắt) + cảnh báo log + override qua biến môi trường. Gating ở UI chỉ là tiện ích, không phải
ranh giới bảo mật (server mới là ranh giới).

## Phương án đã cân nhắc
- **Endpoint `/me` trả vai trò** — loại: JWT đã mang `roles`, giải mã ở client gọn hơn, không tốn
  round-trip; token là nguồn vai trò tin cậy (server ký HS256).
- **Tạo admin bằng SQL thủ công** — loại: không tái lập/demo được, dễ quên.
- **Bật bootstrap admin ở prod** — loại: rủi ro bảo mật (mật khẩu mặc định công khai trong repo);
  prod phải bật tường minh + đặt mật khẩu mạnh.
