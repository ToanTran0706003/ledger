# ADR-0009: Bảo mật & danh tính (JWT, ownership, vai trò)

## Trạng thái
Accepted

## Bối cảnh
Phase 5 thêm xác thực và phân quyền chuẩn doanh nghiệp: ai gọi API, được làm gì, và
không truy cập chéo tài khoản người khác (04-security mục 2–3).

## Quyết định
**Danh tính (IAM):**
- Người dùng là dữ liệu CRUD thường → dùng **JPA** (`UserAccount` + Spring Data
  `UserRepository`, bảng `users` qua Flyway V7). Cố tình tương phản với event store dùng
  JDBC thuần (ADR-0002) — đúng công cụ cho đúng bài toán.
- Mật khẩu băm **BCrypt** (không lưu plaintext). Đăng ký/đăng nhập ở `AuthService`.

**JWT (stateless):**
- Spring Security **OAuth2 Resource Server** xác thực JWT; `NimbusJwtEncoder/Decoder`
  ký/giải **HS256** bằng secret đối xứng (cấu hình `ledger.security.jwt.secret`, override
  qua biến môi trường `LEDGER_JWT_SECRET` ở prod).
- Access token ngắn hạn (15') + refresh token dài hạn (7 ngày), phân biệt bằng claim
  `typ`. Claims: `sub`=userId, `username`, `roles`. `/auth/refresh` cấp lại token.

**Phân quyền:**
- API stateless, không session, CSRF tắt (không dùng cookie).
- `/auth/**` và `/actuator/health` công khai; `/admin/**` và `POST /transactions/*/reverse`
  cần **ADMIN**; `/audit/**` cần **ADMIN hoặc AUDITOR**; còn lại cần đăng nhập.
- **Ownership check** (`AccessControl`): mọi truy cập một `accountId` phải thuộc user đang
  đăng nhập, ADMIN được bỏ qua. Mở tài khoản gán chủ = userId từ token (không nhận từ body).

**Audit:** metadata mỗi event nay gồm cả `userId` (từ SecurityContext) lẫn `correlationId`.

## Hệ quả
**Được:**
- Không truy cập chéo tài khoản (có test 403); phân quyền theo vai trò; mật khẩu an toàn.
- Stateless → dễ scale ngang. Audit truy được "ai làm gì" qua userId trên event.
- Tầng service vẫn test được không cần auth (security thực thi ở tầng web).

**Mất / đánh đổi:**
- HS256 dùng secret đối xứng (đơn giản, đủ cho monolith); lên đa service nên chuyển
  RS256/khóa bất đối xứng hoặc một authorization server riêng.
- Refresh token hiện stateless (chưa có thu hồi/rotation lưu DB) — đủ cho phạm vi hiện tại.
- Mở tài khoản đổi hợp đồng API: bỏ field `owner` trong body (lấy từ token).

## Chưa làm (ghi nhận)
- 🟡 Rate limiting (login/ghi), OWASP Dependency-Check trong CI (chậm/cần NVD key — để sau).
- 🟢 Maker-checker cho giao dịch lớn, 2FA/TOTP.

## Phương án đã cân nhắc
- **Session + cookie** — loại: không stateless, phải chống CSRF, khó scale.
- **Tự viết filter JWT thủ công** — loại: Resource Server của Spring Security chuẩn hơn,
  ít code tự chế dễ sai.
- **Event-source luôn user** — loại: identity là supporting subdomain, CRUD/JPA phù hợp hơn.
