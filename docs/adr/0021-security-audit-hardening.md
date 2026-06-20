# ADR-0021: Rà soát bảo mật toàn dự án & đợt gia cố

## Trạng thái
Accepted

## Bối cảnh
Rà soát bảo mật toàn bộ dự án theo 5 trục (authn/authz, injection/data, secrets/config, lộ thông
tin, crypto/deps). Kết quả: nền tảng tốt — **không có lỗ hổng catastrophic** (không auth-bypass,
không IDOR, không SQLi: mọi JdbcTemplate đều tham số hoá, không polymorphic deserialization,
ownership phủ kín, RBAC chặt, BCrypt-10, JWT HS256 alg-pinned, không RNG yếu). Phát hiện tập trung
ở **một race mất tiền**, **gia cố cấu hình prod**, và **validation biên**.

## Quyết định (đợt gia cố Cao + Trung bình)
1. **Chống duyệt-đôi maker-checker (mất tiền) — #1:** thay "thực thi rồi đánh dấu" bằng **chuyển
   trạng thái nguyên tử** `UPDATE pending_transfers SET status=APPROVED WHERE id=? AND status=PENDING`
   (khoá hàng Postgres). Chỉ lệnh đổi được 1 dòng mới chuyển tiền; lệnh thua → 409. Áp cho cả reject.
   Thay thế hướng `@Version` vì điều kiện UPDATE đơn giản và không cần thêm cột. Test: 8 lệnh duyệt
   song song → đúng 1 thực thi.
2. **Secrets prod fail-fast — #2/#3:** datasource đưa về biến môi trường; `ProdSecurityGuard`
   (`@Profile("prod")`) chặn khởi động nếu JWT secret còn là default dev hoặc DB password còn
   `ledger`. Tránh deploy quên set env rồi chạy bằng secret công khai trong repo.
3. **CORS & actuator theo profile — #4:** CORS đọc `ledger.cors.allowed-origins` (dev `*`, prod siết
   domain qua `LEDGER_CORS_ORIGINS`); prod chỉ expose `health,info`, `health.show-details=when-authorized`.
4. **Lộ thông tin — #7:** thêm Content-Security-Policy; `server.error.*=never` ở prod (không
   stacktrace/message/binding-errors); handler `MethodArgumentNotValid` → 400 gọn. **Không dùng**
   catch-all `@ExceptionHandler(Exception.class)` (nó nuốt `ResponseStatusException` 403/404 → 500).
5. **Validation biên — #6:** `amount` thêm `@Digits(integer=16, fraction=2)` — chặn vượt biên
   `NUMERIC(20,2)` và scale lẻ ngay tầng validation (→ 400) thay vì lọt xuống DB gây 500 + lệch read
   model so với event store.
6. **Login constant-time — #5:** luôn chạy BCrypt (hash giả khi username không tồn tại) → chống
   liệt kê username qua timing. Thông điệp lỗi đã đồng nhất sẵn.
7. **nimbus-jose-jwt:** version 9.37.4 đang dùng **chính là bản vá** CVE-2025-53864 (xác minh NVD:
   nhánh 9.37.x được vá ở 9.37.4) → không bump (tránh phá tương thích Spring Security 6.5/nimbus 9.x).

## Hệ quả
**Được:** bịt lỗ hổng mất tiền nghiêm trọng nhất; cấu hình prod an toàn-mặc-định (fail-fast, không
lộ thông tin); toàn vẹn read model trước input độc hại. 96 test xanh.
**Hoãn có chủ đích (ghi nhận, không trong đợt này):**
- **Token ở localStorage** (rủi ro XSS; TTL access 15' giảm nhẹ) — chuyển cookie HttpOnly là thay đổi
  lớn ở cả FE/BE.
- **Hash-chain SHA-256 không HMAC** → không chống attacker có quyền ghi DB (đã phân tích ở ADR-0014,
  hướng nâng cấp: HMAC + neo ngoài).
- **Register lộ username tồn tại** (đánh đổi UX); **2FA/TOTP** (Phase 5 🟢 còn lại).

## Cập nhật (làm tiếp sau đợt đầu)
- **Refresh token rotation + thu hồi:** bảng `refresh_tokens` (whitelist, id=jti, migration V15);
  refresh **single-use** (tiêu thụ jti cũ nguyên tử rồi cấp mới — phát hiện token lộ/replay);
  `POST /auth/logout` thu hồi mọi refresh token của user (FE gọi khi đăng xuất). Test rotation + logout.
- **Dependabot** (`.github/dependabot.yml`): tự mở PR cập nhật gradle/npm/github-actions — rút ngắn
  cửa sổ CVE so với chỉ quét OWASP theo tuần.

## Phương án đã cân nhắc
- **`@Version` (optimistic lock) cho #1** — loại: UPDATE có điều kiện `WHERE status=PENDING` đạt cùng
  mục tiêu mà không cần thêm cột/version.
- **Catch-all `Exception.class` (theo gợi ý audit)** — loại: nuốt `ResponseStatusException` của
  ownership (403/404 → 500); `server.error.*` + xử lý mặc định của Spring đã đủ an toàn.
- **Bump nimbus lên 10.0.2** — loại: 9.37.4 đã vá; bump major có nguy cơ phá Spring Security 6.5.
