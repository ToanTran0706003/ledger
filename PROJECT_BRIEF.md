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
**Phase 0 ✅ + Phase 1 ✅ (đã verify end-to-end).**
- Phase 0: repo Git, `.gitignore`, `LICENSE`, README, 4 ADR, skeleton Spring Boot 3.5.15/Java 21,
  `application.yml`, `ops/docker-compose.yml`, `/actuator/health` = UP.
- Phase 1 (Walking Skeleton): event store JDBC trên Postgres, `AccountAggregate`+`AccountOpened`,
  command handler, projector → `rm_account_balance`, REST (`POST /accounts`, `GET /accounts/{id}/balance`),
  rebuild read model (`POST /admin/read-model/rebuild`). Test: 4 unit + 3 integration + context, tất cả pass.

**Lưu ý môi trường (xem chi tiết trong code/ADR sau):** máy có sẵn PostgreSQL 18 ở `localhost:5432`
(dùng trực tiếp); Docker Desktop hỏng do WSL nên `docker-compose`/Testcontainers tạm chưa dùng được —
integration test đang chạy trên DB `ledger_test` của Postgres local. Cần JDK 21 (không phải JDK 26)
để chạy Gradle.

**Tiếp theo:** Phase 2 — Core Ledger + Double-Entry (SYSTEM_VAULT, deposit/withdraw/transfer,
integrity check, CI). Xem checklist ở `docs/08-todo-backlog.md`.

## Tài liệu nền
`docs/`: 00 vision · 01 architecture · 02 domain · 03 data/eventstore · 04 security ·
05 performance · 06 uxui · 07 roadmap · 08 todo backlog · 09 tech stack · 10 testing.
