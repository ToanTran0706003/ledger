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
- [x] 🔴 UNIQUE(aggregate_id, aggregate_version) + map lỗi → `ConcurrencyConflict`
- [x] 🔴 Cơ chế retry khi conflict (đọc lại version, validate lại) — RetryingTransactionExecutor (ADR-0006)
- [x] 🔴 Idempotency-Key (IN_PROGRESS/COMPLETED, trả lại response cũ) — IdempotencyService (ADR-0007)
- [x] 🔴 Transactional outbox + relay dispatch in-process (ADR-0006)
- [x] 🔴 Property-based test (jqwik): N giao dịch ngẫu nhiên, assert invariant
- [x] 🔴 Concurrency test: nhiều thread rút cùng account, không âm sai
- [x] 🟡 Test idempotency: gửi 2 lần cùng key → 1 hiệu lực
- [x] 🟡 Test outbox: event đã commit nhưng chưa drain vẫn được relay project

## Phase 4 — Time & Audit
- [x] 🔴 Bảng `snapshots` + ghi snapshot mỗi N event (ADR-0008)
- [x] 🔴 Load aggregate qua snapshot + replay phần sau
- [x] 🔴 Test: xóa snapshot vẫn ra kết quả y hệt
- [x] 🔴 Time-travel: `GET /accounts/{id}/balance?asOf=...`
- [x] 🔴 ReverseTransaction (bút toán bù) + test giữ nguyên lịch sử
- [x] 🟡 Audit metadata (correlationId) trên mọi event — userId/ip để Phase 5 (auth)
- [x] 🟢 Hash chain (per-aggregate SHA-256, metadata trong hash) + `GET /audit/hash-chain` (ADR-0014)

## Phase 5 — Security & Identity
- [x] 🔴 Module IAM: đăng ký/đăng nhập, mật khẩu BCrypt (ADR-0009)
- [x] 🔴 JWT access + refresh, xác thực qua Spring Security Resource Server
- [x] 🔴 Ownership check mọi truy cập accountId (AccessControl)
- [x] 🔴 Vai trò CUSTOMER/ADMIN/AUDITOR + phân quyền endpoint
- [x] 🟡 Security headers (HSTS) + CORS
- [x] 🟡 Rate limiting (token-bucket theo IP: auth chống dò mật khẩu + write) (ADR-0018)
- [x] 🟡 OWASP Dependency-Check trong CI (workflow lên lịch riêng + NVD key) (ADR-0018)
- [x] 🟢 Maker-checker cho giao dịch vượt ngưỡng (four-eyes: người duyệt khác người tạo) (ADR-0020)
- [x] 🟢 2FA/TOTP (RFC 6238, tự cài) — bật/tắt + bắt buộc mã khi đăng nhập (ADR-0021)

## Phase 6 — Observability & Performance
- [x] 🔴 Micrometer + endpoint Prometheus (`/actuator/prometheus`)
- [x] 🔴 Structured logging (JSON ECS, profile prod) + correlationId/userId qua MDC
- [ ] 🟡 Tracing OpenTelemetry (hoãn sang Phase 9 — tracing trả công khi đa service; ADR-0018)
- [ ] 🟡 Dashboard Grafana (chưa làm — Docker hỏng; metric đã sẵn để scrape)
- [x] 🔴 Load test (k6 script + baseline đo thật) → `docs/benchmarks/`
- [x] 🟡 Tối ưu index (idx_events_txid cho reversal); snapshot N cấu hình được (ADR-0010)

## Phase 7 — Frontend (anti-slop)
- [x] 🔴 Khởi tạo frontend (React + TypeScript + Vite) (ADR-0011)
- [x] 🔴 Design tokens (CSS variables) + áp dụng taste-skill chống slop
- [x] 🔴 Đăng nhập/đăng ký, dashboard số dư, nạp/rút/chuyển tiền, sao kê dạng sổ cái
- [x] 🔴 Time-travel viewer + animation replay dựng số dư (signature)
- [x] 🔴 Accessibility AA (tương phản, focus, ARIA), responsive, reduced-motion
- [x] 🟡 Empty states & error states (toast surface message backend)
- [x] 🟡 Copy theo ngôn ngữ người dùng, không sáo rỗng, không em-dash

## Phase 8 — Advanced Business
- [x] 🟡 Tài khoản tiết kiệm + tính lãi qua replay (movementType INTEREST, ADR-0012)
- [x] 🟡 Chuyển tiền định kỳ (scheduler + standing order, ADR-0012)
- [x] 🟡 Hold/reservation (available vs balance) + capture/release + hết hạn tự nhả (ADR-0013)
- [x] 🟢 Fraud detection rule-based (velocity + giao dịch lớn) + auto-freeze + admin freeze/unfreeze (ADR-0015)
- [x] 🟢 Hạn mức giao dịch theo ngày (kiểm tra trong-transaction từ event store, chính xác khi đồng thời) (ADR-0016)

## Phase 9 — Distributed (tùy chọn)
- [x] 🟢 Tách read/write datasource (CQRS): đọc kiểm toán/báo cáo → read pool, prod trỏ replica (ADR-0022)
- [x] 🟢 Kafka làm event backbone (outbox → Kafka → consumer; gated config, Kafka KRaft trong compose) (ADR-0023)
- [x] 🟢 Tách `audit` thành microservice độc lập (consume Kafka, read model + integrity riêng; compose đa-service; event replay rehydrate) (ADR-0024)
- [ ] 🟢 Saga cho transfer liên service + bù trừ khi lỗi (làm tiếp — đã có nền 2 service + Kafka)
- [x] 🟢 Đa tiền tệ + quy đổi tỷ giá (per-currency vault + integrity, FX bắc cầu vault, tỉ giá cấu hình) (ADR-0019)

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
