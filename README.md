# Ledger — Event-Sourced Banking Core

> Một lõi sổ cái tài chính (financial ledger core) xây theo kiến trúc **Event Sourcing + CQRS**, chuẩn doanh nghiệp.
> Business cố tình đơn giản (nạp/rút/chuyển tiền) để dồn sự chú ý vào *cách* xử lý: bất biến dữ liệu, ghi sổ kép, chống race condition, idempotency, audit hoàn hảo, và tua lại lịch sử (time-travel).

[![Java](https://img.shields.io/badge/Java-21_LTS-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green)]()
[![Architecture](https://img.shields.io/badge/Architecture-Event_Sourcing_%2B_CQRS-blue)]()

## Dự án này là gì

Một backend kiểu lõi ngân hàng, nơi **mọi thay đổi tiền là một event bất biến** (không bao giờ UPDATE/DELETE), số dư được *suy ra* từ chuỗi event, và toàn hệ thống tuân thủ **ghi sổ kép (double-entry)** — tiền không tự sinh ra hay mất đi, tổng số dư luôn là hằng số kiểm tra được.

Xem [PROJECT_BRIEF.md](./PROJECT_BRIEF.md) để nắm nhanh bối cảnh, và [docs/](./docs/README.md) cho tài liệu đầy đủ.

## Kiến trúc (tóm tắt)

- **Modular Monolith** — sẵn sàng tách microservices về sau, không làm phức tạp sớm ([ADR-0001](./docs/adr/0001-modular-monolith.md)).
- **Event store trên PostgreSQL** — bảng append-only là nguồn sự thật duy nhất ([ADR-0002](./docs/adr/0002-postgres-event-store.md)).
- **Transactional Outbox** — phát event không mất mát, chưa cần message broker ([ADR-0003](./docs/adr/0003-transactional-outbox.md)).
- **Double-entry + SYSTEM_VAULT** — giải bài "tiền từ đâu ra" ([ADR-0004](./docs/adr/0004-double-entry-system-vault.md)).

## Tech stack

Java 21 (LTS) · Spring Boot 3.5 · Gradle (Kotlin DSL) · PostgreSQL 16 · Flyway · JPA + JDBC · JUnit 5 / Testcontainers. Chi tiết & lý do: [docs/09-tech-stack-and-setup.md](./docs/09-tech-stack-and-setup.md).

## Cấu trúc repo

```
Ledger/
├── PROJECT_BRIEF.md     # bối cảnh dự án (đọc trước)
├── docs/                # tài liệu nền tảng + ADR + benchmarks
├── backend/             # ứng dụng Spring Boot (API)
├── frontend/            # giao diện React + TypeScript (Vite)
└── ops/                 # docker-compose (PostgreSQL), k6 load test
```

## Chạy local

**Yêu cầu:** JDK 21, Node 20+, PostgreSQL (Docker hoặc cài sẵn). Gradle không cần cài (dùng `gradlew`).

```bash
# 1. PostgreSQL (Docker) — hoặc dùng Postgres cài sẵn với DB/role "ledger"
docker compose -f ops/docker-compose.yml up -d

# 2. Backend API (cửa sổ 1)
cd backend
./gradlew bootRun          # Windows: .\gradlew.bat bootRun ; cần JAVA_HOME -> JDK 21
# health: curl http://localhost:8080/actuator/health  ->  {"status":"UP"}

# 3. Frontend (cửa sổ 2)
cd frontend
npm install
npm run dev                # mở http://localhost:5173
```

Đăng ký một tài khoản trong UI, mở account, rồi nạp/rút/chuyển tiền và xem
**replay dựng số dư** + sao kê. Metrics: `http://localhost:8080/actuator/prometheus`.

## Trạng thái hiện tại

**Phase 0 → 7 đã xong** (backend ES/CQRS đầy đủ + frontend). Xem chi tiết ở
[PROJECT_BRIEF.md](./PROJECT_BRIEF.md) và lộ trình ở [docs/07-roadmap-and-phases.md](./docs/07-roadmap-and-phases.md).

## License

[MIT](./LICENSE)
