# 08 — TODO Backlog

> Checklist thi công chi tiết, bám theo các phase ở `07`. Đánh dấu `[x]` khi xong. Mỗi mục nên trở thành một issue/PR nhỏ.

## Quy ước
- 🔴 Bắt buộc (lõi giá trị) · 🟡 Nên có · 🟢 Tùy chọn/nâng cao
- Mỗi task xong cần: code + test (nếu áp dụng) + cập nhật docs liên quan.

---

## Phase 0 — Foundation
- [x] 🔴 Tạo repo Git, cấu trúc thư mục, `.gitignore`, `LICENSE`
- [x] 🔴 README gốc (mô tả ngắn + link docs + cách chạy)
- [x] 🔴 Chép bộ docs này vào `docs/`
- [x] 🔴 Khởi tạo Spring Boot (Gradle, Java 21) chạy được endpoint health
- [x] 🔴 docker-compose: PostgreSQL local (*file đã tạo; máy hiện chạy PG local trực tiếp vì Docker/WSL hỏng*)
- [ ] 🟡 Spotless + Checkstyle, format nhất quán
- [x] 🟡 Viết ADR-0001 (modular monolith), 0002 (PG event store), 0003 (outbox), 0004 (double-entry vault)
- [ ] 🟢 Pre-commit hook chạy format + test nhanh

## Phase 1 — Walking Skeleton
- [x] 🔴 `DomainEvent` (interface), `AbstractAggregate` (apply/replay), `Command`
- [x] 🔴 `EventStore` interface + impl Postgres (append, loadStream)
- [x] 🔴 Migration Flyway: bảng `events`
- [x] 🔴 `AccountAggregate` + event `AccountOpened`
- [x] 🔴 `OpenAccountCommandHandler`
- [x] 🔴 Load aggregate bằng replay event
- [x] 🔴 `Projector` + read model `rm_account_balance` (migration)
- [x] 🔴 REST: `POST /accounts`, `GET /accounts/{id}/balance`
- [x] 🔴 Integration test: mở tài khoản → số dư 0; replay dựng đúng
- [x] 🟡 Endpoint rebuild read model (admin) + test rebuild ra kết quả y hệt

## Phase 2 — Core Ledger + Double-Entry
- [x] 🔴 Khái niệm Posting/Transaction cân vế (MoneyPosted, ADR-0005)
- [x] 🔴 Seed `SYSTEM_VAULT` số dư khởi tạo (posting GENESIS lúc khởi động)
- [x] 🔴 Command + event: Deposit, Withdraw, Transfer
- [x] 🔴 Invariant số dư CUSTOMER ≥ 0 (vault được âm)
- [x] 🔴 Read model `rm_transaction_history`
- [x] 🔴 REST cho deposit/withdraw/transfer + xem lịch sử
- [x] 🔴 `GET /audit/integrity` (tổng số dư == hằng số)
- [x] 🔴 Test: chuyển tiền cân vế, không âm, integrity pass
- [x] 🔴 GitHub Actions: build + test
- [x] 🟡 Validation đầu vào (Bean Validation) + lỗi rõ ràng (ProblemDetail)

## Phase 3 — Correctness under Pressure
- [ ] 🔴 UNIQUE(aggregate_id, aggregate_version) + map lỗi → `ConcurrencyConflict`
- [ ] 🔴 Cơ chế retry khi conflict (đọc lại version, validate lại)
- [ ] 🔴 Bảng + filter Idempotency-Key (IN_PROGRESS/COMPLETED, trả lại response cũ)
- [ ] 🔴 Transactional outbox + relay dispatch in-process
- [ ] 🔴 Property-based test (jqwik): N giao dịch ngẫu nhiên, assert 3 invariant
- [ ] 🔴 Concurrency test: nhiều thread rút cùng account, không âm sai
- [ ] 🟡 Test idempotency: gửi 2 lần cùng key → 1 hiệu lực
- [ ] 🟡 Test outbox: crash giữa chừng không mất event

## Phase 4 — Time & Audit
- [ ] 🔴 Bảng `snapshots` + ghi snapshot mỗi N event
- [ ] 🔴 Load aggregate qua snapshot + replay phần sau
- [ ] 🔴 Test: xóa snapshot vẫn ra kết quả y hệt
- [ ] 🔴 Time-travel: `GET /accounts/{id}/balance?asOf=...`
- [ ] 🔴 ReverseTransaction (event bù) + test giữ nguyên lịch sử
- [ ] 🟡 Audit metadata (userId, ip, correlationId) trên mọi event
- [ ] 🟢 Hash chain + endpoint verify chuỗi toàn vẹn

## Phase 5 — Security & Identity
- [ ] 🔴 Module IAM: đăng ký/đăng nhập, mật khẩu Argon2/BCrypt
- [ ] 🔴 JWT access + refresh, filter xác thực
- [ ] 🔴 Ownership check mọi truy cập accountId
- [ ] 🔴 Vai trò CUSTOMER/ADMIN/AUDITOR + phân quyền endpoint
- [ ] 🟡 Rate limiting (login + ghi), security headers, CORS đúng
- [ ] 🟡 OWASP Dependency-Check trong CI
- [ ] 🟢 Maker-checker cho giao dịch vượt ngưỡng
- [ ] 🟢 2FA/TOTP cho thao tác nhạy cảm

## Phase 6 — Observability & Performance
- [ ] 🔴 Micrometer + endpoint Prometheus
- [ ] 🔴 Structured logging (JSON) + correlationId
- [ ] 🟡 Tracing OpenTelemetry
- [ ] 🟡 Dashboard Grafana (latency, throughput, conflict rate, projection lag)
- [ ] 🔴 Load test k6/Gatling + ghi biểu đồ p99 vào `docs/benchmarks/`
- [ ] 🟡 Tối ưu index/snapshot N theo kết quả; thêm cache nếu đo thấy nghẽn

## Phase 7 — Frontend (anti-slop)
- [ ] 🔴 Khởi tạo frontend (React/Next hoặc tương đương)
- [ ] 🔴 Design tokens + design notes (giải thích lựa chọn, tự phản biện slop)
- [ ] 🔴 Đăng nhập, dashboard số dư, chuyển tiền, sao kê
- [ ] 🔴 Time-travel viewer + animation replay dựng số dư (signature)
- [ ] 🔴 Accessibility AA, responsive, reduced-motion
- [ ] 🟡 Empty states & error states hướng dẫn được
- [ ] 🟡 Loại bỏ copy sáo rỗng, dùng ngôn ngữ người dùng

## Phase 8 — Advanced Business
- [ ] 🟡 Tài khoản tiết kiệm + tính lãi qua replay (InterestAccrued)
- [ ] 🟡 Chuyển tiền định kỳ (scheduler + standing order)
- [ ] 🟡 Hold/reservation + hết hạn tự nhả
- [ ] 🟢 Fraud detection rule-based + FraudAlertRaised + auto-freeze
- [ ] 🟢 Hạn mức giao dịch theo ngày

## Phase 9 — Distributed (tùy chọn)
- [ ] 🟢 Tách read DB / write DB
- [ ] 🟢 Kafka làm event backbone (thay outbox in-process)
- [ ] 🟢 Tách 1–2 module thành microservice
- [ ] 🟢 Saga cho transfer liên service + bù trừ khi lỗi
- [ ] 🟢 Đa tiền tệ + quy đổi tỷ giá

## Phase 10 — Polish & Storytelling
- [ ] 🔴 README hoàn chỉnh: chạy < 15 phút, sơ đồ, GIF demo
- [ ] 🟡 Trang docs (Docusaurus/GitHub Pages)
- [ ] 🟡 Video demo ngắn các tính năng wao
- [ ] 🟢 Series blog về quyết định kiến trúc & bài học

---

## Backlog xuyên suốt (làm liên tục, không thuộc 1 phase)
- [ ] 🔴 Cập nhật docs mỗi khi quyết định thay đổi
- [ ] 🔴 Mỗi quyết định lớn → ADR mới
- [ ] 🔴 Self-review & explain cuối mỗi phase (ghi lại điều học được)
- [ ] 🟡 Giữ coverage test ở mức ý nghĩa (ưu tiên test hành vi nghiệp vụ)
- [ ] 🟡 Cập nhật "Trạng thái hiện tại" trong `docs/README.md`

## Bước kế tiếp
Đọc `09-tech-stack-and-setup.md` để dựng môi trường và bắt đầu code Phase 0–1.
