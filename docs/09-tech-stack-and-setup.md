# 09 — Tech Stack & Setup

> Mọi lựa chọn đều kèm *lý do*. Mục tiêu: chuẩn doanh nghiệp, budget $0, dễ cho người chuyển từ C#/.NET sang.

## 1. Vì sao Java/Spring hợp với người đến từ C#
ASP.NET Core và Spring Boot gần như song ánh về khái niệm: DI container, middleware/filter, controller, ORM (EF Core ↔ JPA/Hibernate), config theo môi trường. Bạn chuyển kiến thức rất nhanh; phần mới chủ yếu là cú pháp và hệ sinh thái build.

## 2. Stack đề xuất (kèm lý do)

| Lớp | Lựa chọn | Lý do |
|-----|----------|-------|
| Ngôn ngữ | **Java 21 (LTS)** | LTS, records/sealed/pattern matching rất hợp mô hình event & command |
| Framework | **Spring Boot 3.x** | Chuẩn ngành Java enterprise, fintech tuyển nhiều |
| Build | **Gradle (Kotlin DSL)** | Nhanh, hiện đại; Maven cũng được nếu bạn quen |
| DB | **PostgreSQL** | JSONB cho payload event, mạnh mẽ, free |
| Migration | **Flyway** | Versioned migration, chuẩn doanh nghiệp |
| Truy cập DB | **Spring Data JPA + JdbcTemplate** | JPA cho read model; JDBC thuần cho event store (kiểm soát chặt) |
| Bảo mật | **Spring Security + JWT** | Chuẩn, đầy đủ |
| Test | **JUnit 5, Testcontainers, jqwik, REST Assured** | Test thật trên Postgres container; property-based test |
| Quan sát | **Micrometer + Prometheus + Grafana, OpenTelemetry** | Metrics/tracing chuẩn |
| Chất lượng | **Spotless + Checkstyle, OWASP Dependency-Check** | Ép chất lượng tự động |
| Đóng gói | **Docker + docker-compose** | Chạy local một lệnh, deploy dễ |
| Event backbone (Phase 9) | **Kafka** | Chỉ đưa vào khi lên distributed |
| Cache (khi cần) | **Redis** | Thêm khi đo thấy nghẽn, không sớm |
| Frontend (Phase 7) | **React/Next + TypeScript** | Phổ biến, dễ tuyển, dễ làm UI tốt |

> **Nguyên tắc:** không thêm công nghệ vì "nghe pro". Mỗi thứ vào stack phải giải quyết một vấn đề *đã có*. Kafka/Redis/microservices chỉ xuất hiện ở phase sau khi có lý do đo được.

## 3. Cấu trúc thư mục repo (gợi ý)
```
Ledger/
├── docs/                      # bộ tài liệu này
│   ├── adr/                   # architecture decision records
│   └── benchmarks/            # kết quả load test
├── backend/
│   ├── src/main/java/com/ledger/...   # theo module ở file 01
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/      # Flyway
│   └── src/test/java/...
├── frontend/                  # Phase 7
├── ops/
│   ├── docker-compose.yml
│   └── grafana/ prometheus/
├── .github/workflows/         # CI
├── README.md
└── LICENSE
```

## 4. Khởi tạo nhanh (Phase 0)
1. Tạo project Spring Boot (qua [start.spring.io](https://start.spring.io)) với dependencies: Web, Data JPA, PostgreSQL Driver, Flyway, Validation, Security, Actuator.
2. Viết `docker-compose.yml` chạy PostgreSQL.
3. Cấu hình `application.yml` kết nối DB + Actuator health.
4. Chạy `./gradlew bootRun`, kiểm tra `/actuator/health`.
5. Commit, đẩy lên GitHub (repo public để nhà tuyển dụng xem).

## 5. Deploy $0 (free tier)
| Thành phần | Lựa chọn miễn phí |
|-----------|-------------------|
| Backend | Railway / Render / Fly.io (free tier) |
| PostgreSQL | Supabase / Neon (free) |
| Frontend | Vercel / Netlify (free) |
| CI/CD | GitHub Actions (free cho public repo) |
| Observability local | Grafana/Prometheus qua docker-compose |

> Lưu ý free tier có thể "ngủ" khi không dùng — chấp nhận được với demo portfolio. Ghi rõ trong README để reviewer không hiểu nhầm là lỗi.

## 6. Về việc dùng AI làm công cụ phát triển
Dự án phát triển với sự hỗ trợ của AI. Để giữ chất lượng và tránh "slop":
- **Hiểu mọi dòng code** trước khi commit (xem `06` mục Anti-slop cho code).
- Nếu muốn AI thao tác trực tiếp trên repo trong máy (đọc/sửa file, chạy lệnh), công cụ phù hợp là **Claude Code** — chạy trên máy bạn và có quyền truy cập thư mục dự án. Tham khảo tài liệu chính thức tại https://docs.anthropic.com/en/docs/claude-code để biết cách cài đặt và yêu cầu hệ thống (thông tin phiên bản thay đổi theo thời gian, nên xem trực tiếp tại nguồn).
- Giao diện chat thông thường không truy cập được ổ đĩa máy bạn; nó tạo file để bạn tải về và chép vào repo.

## 7. Bước kế tiếp
Đọc `10-testing-and-quality.md`, rồi bắt tay vào Phase 0 theo `08-todo-backlog.md`.
