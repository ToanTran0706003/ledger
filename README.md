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
├── docs/                # tài liệu nền tảng + ADR
├── backend/             # ứng dụng Spring Boot
└── ops/                 # docker-compose (PostgreSQL), hạ tầng local
```

## Chạy local

**Yêu cầu:** JDK 21, Docker Desktop. (Gradle không cần cài — dùng wrapper `gradlew` kèm sẵn.)

```bash
# 1. Khởi động PostgreSQL
docker compose -f ops/docker-compose.yml up -d

# 2. Chạy ứng dụng (từ thư mục backend/)
cd backend
./gradlew bootRun          # Windows: .\gradlew.bat bootRun

# 3. Kiểm tra health
curl http://localhost:8080/actuator/health
# -> {"status":"UP", ...}
```

> Trên Windows, nếu có nhiều JDK, đảm bảo `JAVA_HOME` trỏ về JDK 21 trước khi chạy `gradlew`.

## Trạng thái hiện tại

**Phase 0 — Foundation.** Skeleton chạy được, health check UP. Tiếp theo: Phase 1 — Walking Skeleton (Event Sourcing tối thiểu). Xem lộ trình ở [docs/07-roadmap-and-phases.md](./docs/07-roadmap-and-phases.md).

## License

[MIT](./LICENSE)
