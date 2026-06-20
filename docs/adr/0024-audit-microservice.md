# ADR-0024: Tách `audit` thành microservice độc lập (Phase 9)

## Trạng thái
Accepted

## Bối cảnh
Phase 9: tách một module thành service triển khai độc lập. Sau khi có **Kafka event backbone**
(ADR-0023), ứng viên rủi ro thấp nhất là **audit** — chỉ ĐỌC, là downstream tự nhiên của luồng event.

## Quyết định
- **`services/audit-service/`** là một **Spring Boot độc lập** (build riêng, cổng 8081, DB riêng
  `ledger_audit`), KHÔNG sửa monolith `backend/` (105+ test giữ nguyên, CI core không đổi).
- **Event-driven:** audit-service consume topic Kafka `ledger.events` (envelope `{eventType,payload}`),
  tự **dựng lại read model riêng** (`audit_account_balance`) từ `AccountOpened` + `MoneyPosted`.
  Idempotent (UPSERT) vì giao nhận at-least-once.
- **Kiểm toán độc lập:** expose `GET /audit/integrity` — tổng số dư theo từng tiền tệ do CHÍNH service
  này tính, đối chiếu seed. Hai view dựng độc lập (core projector vs audit-service) cùng "cân" =
  bằng chứng toàn vẹn mạnh hơn một nguồn.
- **Hạ tầng:** `ops/docker-compose.yml` chạy postgres + kafka + ledger-core + audit-service. Core bật
  `ledger.kafka.enabled=true` để publish; audit-service là consumer nhóm `audit-service`.

## Hệ quả
**Được:** một service thứ hai triển khai độc lập, tiêu thụ event qua Kafka — chứng minh tách
microservice + decoupling qua event backbone, không đụng lõi ledger. Sẵn nền cho Saga (ADR sau).
**Mất / giới hạn:**
- DB riêng cho audit nhưng cùng instance Postgres trên máy dev (một máy); prod tách instance.
- Chưa có API gateway/định tuyến chung — gọi trực tiếp 8080 (core) / 8081 (audit). Gateway là bước sau.
- audit-service không xác thực JWT (service nội bộ sau gateway) — nêu rõ; prod đặt sau mạng quản trị.

## Phương án đã cân nhắc
- **Tách `account`/money core thành service:** loại (bước này) — rủi ro cao cho invariant "sổ luôn
  cân"; audit read-only an toàn hơn để chứng minh mô hình trước.
- **audit đọc trực tiếp DB của core:** loại — coupling DB. Consume Kafka + read model riêng đúng tinh
  thần microservice (chia sẻ nothing, decouple qua event).
