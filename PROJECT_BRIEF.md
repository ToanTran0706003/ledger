# PROJECT BRIEF — Ledger

> File này là bối cảnh nền cho **mọi phiên làm việc** (Claude Code hoặc người mới vào dự
> án). Đọc file này trước, rồi đọc `docs/`. Cập nhật khi có quyết định lớn thay đổi.

## Về người phát triển
- Web developer ~1 năm kinh nghiệm **C#/.NET**, đang chuyển sang **Java** để làm dự án
  portfolio gây ấn tượng với nhà tuyển dụng (đặc biệt **fintech**).
- Phát triển với sự hỗ trợ của AI, nhưng phải **chuẩn doanh nghiệp** và **hiểu/giải thích
  được mọi quyết định**.

## Dự án
**Ledger** — lõi sổ cái tài chính (financial ledger core) xây bằng **Event Sourcing +
CQRS**. Business cố tình đơn giản (nạp/rút/chuyển tiền); độ khó dồn vào kỹ thuật:
event store, double-entry, concurrency, idempotency, time-travel, audit.

## Quyết định kiến trúc đã chốt
| # | Quyết định | ADR |
|---|------------|-----|
| 1 | **Modular Monolith** (sẵn sàng tách microservices sau, KHÔNG làm ngay) | `docs/adr/0001-modular-monolith.md` |
| 2 | **PostgreSQL** làm event store (bảng append-only), không EventStoreDB | `docs/adr/0002-postgres-event-store.md` |
| 3 | **Transactional Outbox** thay vì message broker ở giai đoạn đầu | `docs/adr/0003-transactional-outbox.md` |
| 4 | **Double-entry** với tài khoản **SYSTEM_VAULT** để giải "tiền từ đâu ra" | `docs/adr/0004-double-entry-system-vault.md` |
| 5 | **Account-centric postings** (`MoneyPosted`) cho double-entry | `docs/adr/0005-account-centric-postings.md` |
| 6 | **Transactional outbox + retry** (read-your-writes qua drain sau commit) | `docs/adr/0006-transactional-outbox-and-retry.md` |
| 7 | **Idempotency-Key** cho endpoint ghi tiền | `docs/adr/0007-idempotency-keys.md` |
| 8 | **Snapshot + time-travel + reversal** (audit metadata correlationId) | `docs/adr/0008-snapshots-time-travel-reversal.md` |
| 9 | **Security**: JWT (HS256) + ownership check + vai trò, IAM dùng JPA | `docs/adr/0009-security-and-identity.md` |
| 10 | **Observability & Perf**: Prometheus metrics, structured logs, benchmark | `docs/adr/0010-observability-and-performance.md` |
| 11 | **Frontend** React+TS+Vite, design tokens anti-slop (taste-skill) | `docs/adr/0011-frontend-and-design-system.md` |
| 12 | **Advanced business**: tiết kiệm/lãi qua replay, lệnh định kỳ | `docs/adr/0012-advanced-business.md` |

Mỗi quyết định lớn mới → ghi thêm một ADR vào `docs/adr/` (template ở `01-architecture.md`).

## Stack
Java 21 (LTS) · Spring Boot 3.x · Gradle (Kotlin DSL) · PostgreSQL · Flyway ·
Spring Data JPA (read model) + JdbcTemplate/JDBC thuần (event store) · Spring Security + JWT ·
JUnit 5 + Testcontainers + jqwik + REST Assured · Micrometer/Prometheus/Grafana + OpenTelemetry ·
Docker + docker-compose. Kafka/Redis/microservices chỉ vào ở phase sau khi có lý do đo được.

## Cấu trúc module (package by feature)
`com.ledger` → `shared/` (eventstore, domain, idempotency, observability) · `account/` ·
`ledger/` · `audit/` · `iam/`. Mỗi module: `domain / command / query / projection / api`.
**Module chỉ giao tiếp qua public interface hoặc domain event** — không gọi internal của nhau.

## Nguyên tắc làm việc (người dùng yêu cầu)
1. **Đọc docs liên quan trước khi code**, bám sát chúng.
2. **Giải thích vì sao** cho mỗi quyết định, không chỉ viết code.
3. **Vertical slice:** mỗi phase chạy được end-to-end + có test trước khi sang phase sau.
4. **Anti-slop:** không abstraction thừa, tên theo domain, không comment thừa.
5. **Dạy người từ C#** những chỗ Java/Spring khác .NET.

## Lộ trình (tóm tắt — chi tiết ở `docs/07` và `docs/08`)
P0 Foundation → P1 Walking Skeleton → P2 Double-Entry → P3 Correctness →
P4 Time & Audit → P5 Security → P6 Observability/Perf → P7 Frontend →
P8 Advanced Business → P9 Distributed → P10 Polish.
Điểm dừng an toàn: sau **P4** đã đủ ấn tượng cho phỏng vấn.

## Trạng thái hiện tại
**Phase 0 → 8 (P8 làm một phần) ✅ — verify end-to-end, CI xanh hai tầng.** Repo public:
https://github.com/ToanTran0706003/ledger · 47 backend test · 12 ADR · README capstone.
Đã áp dụng 3 plugin: frontend-design (typography/depth), superpowers (TDD red→green), code-review.
- Phase 0: skeleton Spring Boot 3.5.15/Java 21, `/actuator/health` = UP.
- Phase 1: event store JDBC, AccountAggregate+AccountOpened, projector → rm_account_balance, rebuild.
- Phase 2: double-entry account-centric (MoneyPosted, ADR-0005), SYSTEM_VAULT GENESIS, deposit/
  withdraw/transfer, invariant không-âm, rm_transaction_history, integrity check, ProblemDetail, CI.
- Phase 3 (Correctness): transactional outbox + OutboxRelay (read-your-writes qua drain sau commit),
  RetryingTransactionExecutor (retry trên ConcurrencyConflict), Idempotency-Key (ADR-0006/0007).
- Phase 4 (Time & Audit): snapshot mỗi N event (load qua snapshot + replay sau), time-travel
  (`GET /accounts/{id}/balance?asOf=...`), reversal (bút toán bù, `POST /transactions/{txId}/reverse`),
  audit metadata correlationId trên mọi event (ADR-0008).
- Phase 5 (Security): IAM (JPA, BCrypt), JWT access+refresh (Spring Security Resource Server, HS256),
  ownership check (không truy cập chéo tài khoản), vai trò CUSTOMER/ADMIN/AUDITOR, userId vào audit
  metadata (ADR-0009). `/auth/register|login|refresh`. Endpoint mở tài khoản lấy chủ từ token.
- Phase 6 (Observability & Perf): Prometheus `/actuator/prometheus` (metrics: command latency,
  throughput, conflict rate, projection lag), structured JSON log (profile prod), index reversal,
  k6 load script + benchmark thật (ghi p99 ≈ 14.6ms, đọc p99 ≈ 4.3ms — vượt mục tiêu) (ADR-0010).
- Phase 7 (Frontend): React + TypeScript + Vite (`frontend/`), design tokens tự dựng (anti-slop,
  áp dụng skill taste-skill). **5 màn**: đăng nhập, bảng điều khiển (chỉ số + integrity badge +
  feed), chi tiết tài khoản (replay + biểu đồ số dư SVG + slider time-travel + sao kê), chuyển tiền,
  kiểm toán (invariant double-entry). Signature "replay dựng số dư từ chuỗi sự kiện", accessibility
  AA, reduced-motion (ADR-0011). Backend thêm `GET /accounts` + `/audit/integrity` cho user. CI build cả frontend.
- Phase 8 (Advanced Business): tài khoản tiết kiệm + tính lãi qua replay (time-weighted, lãi từ
  vault giữ integrity), lệnh chuyển tiền định kỳ (standing order + scheduler, at-most-once) (ADR-0012).
  Làm theo TDD + một lượt /code-review (phát hiện & sửa: bug cắt dưới giây khi tính lãi, clamp asOf,
  at-most-once cho standing order, validate tài khoản nhận, xóa dead code).
  **Đã surface lên UI**: mở tài khoản Tiết kiệm (chọn loại), màn "Định kỳ" tạo/liệt kê lệnh —
  scheduler tự chuyển tiền (đã verify trực quan). Read model thêm cột account_type (migration V10).
  Frontend giờ **6 màn** (thêm Định kỳ).
- Backend test: **47 test, 0 fail** (jqwik, concurrency, security MockMvc, metrics, interest, standing order).

**Lưu ý môi trường:** máy có sẵn PostgreSQL 18 ở `localhost:5432` (dùng trực tiếp); Docker Desktop
hỏng do WSL nên `docker-compose`/Testcontainers tạm chưa dùng được — integration test local chạy
trên DB `ledger_test`; CI thì dùng Postgres service container. Cần JDK 21 (không phải JDK 26) chạy Gradle.

## Việc đáng làm tiếp (backlog ý tưởng — cho phiên sau)
Ưu tiên gợi ý từ trên xuống:
1. **Hoàn tất Phase 8 backend** (chiều sâu nghiệp vụ):
   - **Hold/reservation**: số dư khả dụng (available) vs thực (balance); đặt giữ → capture/release;
     hết hạn tự nhả (scheduler). *Lưu ý:* đụng invariant lõi — debit phải tôn trọng available = balance − held.
   - **Fraud detection** rule-based (velocity, giao dịch lớn bất thường) → `FraudAlertRaised` + auto-freeze.
   - **Hạn mức giao dịch/ngày** (đọc rm_transaction_history).
2. **Surface lãi tiết kiệm rõ trên UI**: trang/nút ADMIN "tính lãi" (accrue), hoặc job lịch hằng ngày;
   hiện dòng "Lãi" trong sao kê (label đã có).
3. **Nhúng ảnh/GIF demo vào README** (cần commit ảnh vào repo) — tăng thuyết phục với reviewer.
4. **Phase 9 — Distributed**: tách read/write DB, Kafka thay outbox in-process, tách 1–2 module thành
   microservice, Saga cho giao dịch liên service.
5. **Hoàn thiện Phase 5/6 còn nợ**: rate limiting (login/ghi), OWASP Dependency-Check trong CI,
   OpenTelemetry tracing, dashboard Grafana (cần sửa Docker/WSL — xem [[ledger-dev-environment]]).
6. **Dọn nợ nhỏ**: điểm nóng SYSTEM_VAULT (shard/giảm contention) khi đo thấy nghẽn; refresh-token
   rotation/thu hồi; RS256 thay HS256 khi lên đa service.

Chi tiết lộ trình gốc: `docs/07-roadmap-and-phases.md` và `docs/08-todo-backlog.md` (đã tick tới P8).

## Tài liệu nền
`docs/`: 00 vision · 01 architecture · 02 domain · 03 data/eventstore · 04 security ·
05 performance · 06 uxui · 07 roadmap · 08 todo backlog · 09 tech stack · 10 testing.
