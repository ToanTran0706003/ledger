# ADR-0026: Distributed tracing (OpenTelemetry) xuyên service (Phase 6/9)

## Trạng thái
Accepted

## Bối cảnh
Tracing được hoãn từ Phase 6 ("trả công khi đa service"). Nay đã có 4 service (ledger-core,
audit-service, saga-orchestrator, compliance-service) + Docker chạy → một request đi xuyên nhiều
service, cần trace end-to-end để quan sát/độ trễ/gỡ lỗi phân tán.

## Quyết định
- **Micrometer Tracing + cầu OpenTelemetry + OTLP exporter** trên **cả 4 service**
  (`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`). Mỗi service đặt
  `spring.application.name` (= service.name trong trace), `management.tracing.sampling.probability: 1.0`
  (demo: trace tất cả), `management.otlp.tracing.endpoint` (env `OTLP_ENDPOINT`, mặc định
  `http://localhost:4318/v1/traces`).
- **Backend Jaeger all-in-one** trong `ops/docker-compose.yml` (UI 16686, OTLP HTTP 4318).
- **Lan truyền context:** HTTP qua header W3C `traceparent` (RestClient của orchestrator được
  Spring Boot tự instrument — đã **bỏ bean `RestClientConfig` tự chế** vì nó tạo `RestClient.builder()`
  trần KHÔNG instrument, sẽ làm gãy propagation); Kafka qua header bản ghi (Micrometer tự instrument
  producer ledger-core + consumer audit-service).
- **Test im lặng:** `management.tracing.enabled: false` ở profile test (không export khi không có collector).

## Hệ quả
**Được:** một request Saga được trace **end-to-end qua 3 service** (verify thật: 1 trace = 17 span,
service = saga-orchestrator + ledger-core + compliance-service trong Jaeger). Quan sát độ trễ từng
bước, gỡ lỗi phân tán. Hoàn tất hạng mục tracing (Phase 6).
**Mất / giới hạn:**
- Sampling 100% chỉ hợp demo; prod nên giảm (vd 0.1) + tail-sampling.
- Chưa gom log+trace (log correlation qua traceId) — bước nâng cấp.

## Phương án đã cân nhắc
- **Zipkin thay Jaeger:** tương đương; Jaeger phổ biến + OTLP sẵn.
- **Tự truyền traceparent thủ công:** loại — Micrometer/OTel tự instrument HTTP/Kafka, ít code, chuẩn W3C.
