# ADR-0023: Kafka làm event backbone (Phase 9)

## Trạng thái
Accepted

## Bối cảnh
Phase 9: thay đường phân phối event in-process bằng một backbone (Kafka) để chuẩn bị cho nhiều
consumer / tách service. Docker đã chạy lại được nên có thể chạy Kafka thật (KRaft) qua
`ops/docker-compose.yml`.

## Quyết định
- **Giữ transactional outbox (nguồn tin cậy), thêm Kafka làm đường phân phối** — mẫu chuẩn
  *outbox → Kafka → consumer*. `OutboxRelay` không còn projection trực tiếp mà gọi `EventPublisher`:
  - **In-process** (mặc định, Kafka tắt): projection chạy ngay trong transaction outbox —
    "effectively-once" như trước (ADR-0006), không cần hạ tầng ngoài.
  - **Kafka** (bật): publish event (gói `EventEnvelope{eventType,payload}`) lên topic `ledger.events`
    đồng bộ TRONG transaction outbox rồi mới đánh dấu SENT; lỗi gửi → rollback → thử lại
    (**at-least-once**). `KafkaProjectionConsumer` (`@KafkaListener`) nhận và projection bất đồng bộ;
    **idempotent** vì projector dùng UPSERT.
- **Bật/tắt bằng cấu hình** `ledger.kafka.enabled` (mặc định tắt). Toàn bộ bean Kafka
  (`@ConditionalOnProperty`) chỉ nạp khi bật → khi tắt không cần broker, mọi test/đời chạy như cũ.
- **Topic 1 partition** (demo): tổng thứ tự event → projection áp đúng trình tự. Production: nhiều
  partition + key = aggregateId để giữ thứ tự theo từng aggregate mà vẫn song song hoá.
- **Hạ tầng:** Kafka KRaft single-node (không Zookeeper) trong `ops/docker-compose.yml`.

## Hệ quả
**Được:** event backbone thật, sẵn sàng cho nhiều consumer / tách service (bước tiếp theo: microservice
+ Saga). Mặc định vẫn in-process nên không ép hạ tầng cho ai chỉ muốn chạy nhanh. Test end-to-end qua
**Kafka nhúng (in-JVM)** — CI không cần Docker.
**Mất / giới hạn:**
- Khi bật Kafka, projection là **eventually-consistent** (async) — đọc-ngay-sau-ghi có thể trễ; phù
  hợp CQRS, nhưng các test cũ (đọc-thấy-ghi tức thì) chỉ đúng ở chế độ in-process mặc định.
- 1 partition giới hạn throughput; nâng cấp như trên.
- Chưa có dead-letter topic / retry policy tinh chỉnh (mặc định Spring Kafka) — đủ cho phạm vi hiện tại.

## Phương án đã cân nhắc
- **Bỏ outbox, publish thẳng Kafka từ command:** loại — mất bảo đảm "không rơi event" khi commit DB
  thành công nhưng publish lỗi. Outbox → Kafka giữ tin cậy.
- **Testcontainers Kafka cho test:** loại (cho test) — Kafka nhúng nhẹ hơn, nhanh hơn, không cần
  Docker ở CI. Docker để chạy thật ở local/demo.
