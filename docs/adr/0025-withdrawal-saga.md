# ADR-0025: Saga rút tiền cần duyệt tuân thủ (Phase 9)

## Trạng thái
Accepted

## Bối cảnh
Phase 9 mục cuối: Saga cho thao tác liên service + bù trừ (compensation) khi lỗi. Đã có nền:
ledger-core + audit-service + Kafka. Cần một quy trình GHI nhiều bước, nhiều service, có thể thất bại
ở bước phân tán và phải hoàn tác.

## Quyết định
- **Saga điều phối (orchestration)** cho "rút tiền cần duyệt tuân thủ", **tái dùng cơ chế hold** của
  core (ADR-0013) làm bước reserve/commit/compensate — **KHÔNG sửa ledger-core** (gọi REST API sẵn có):
  1. **Reserve:** orchestrator gọi core `POST /accounts/{id}/holds` → giữ tiền (giảm available).
  2. **Decide:** orchestrator gọi `compliance-service` `POST /compliance/evaluate` → approved/rejected.
  3a. **Approved →** `POST .../holds/{holdId}/capture` (core commit lệnh rút). Kết quả COMPLETED.
  3b. **Rejected →** `POST .../holds/{holdId}/release` (**bù trừ** — trả lại tiền). Kết quả REJECTED.
  3c. **Lỗi giữa chừng** (compliance chết, capture lỗi) → **release** hold (bù trừ best-effort) → FAILED.
- **Ba service:** `saga-orchestrator` (8082, điều phối + bù trừ), `compliance-service` (8083, quyết
  định theo ngưỡng), `ledger-core` (8080, thực thi hold/capture/release). Orchestrator **chuyển tiếp
  JWT** của người gọi xuống core (core vẫn enforce ownership).
- **Vì sao orchestration (REST) chứ không choreography (Kafka):** quy trình ngắn, cần kết quả đồng bộ
  cho người gọi + điểm bù trừ rõ ràng tập trung → orchestration dễ hiểu/giám sát/kiểm thử hơn. (Bước sau
  có thể chuyển sang choreography qua Kafka khi cần độ bền/đồng thời cao.)
- **Bù trừ là trọng tâm:** `release` hold là compensating transaction — đảm bảo không "kẹt tiền giữ"
  khi từ chối/lỗi. Hold idempotent + có TTL tự nhả là lưới an toàn cuối.

## Hệ quả
**Được:** Saga liên service thật + bù trừ, không đụng lõi ledger (rủi ro thấp), tái dùng hold. Hoàn tất
Phase 9 (đa tiền tệ + read/write split + Kafka backbone + microservice + Saga).
**Mất / giới hạn:**
- Orchestrator là điểm điều phối tập trung (không phải SPOF nghiêm trọng vì hold có TTL tự nhả bù lưới).
- Đồng bộ REST: nếu orchestrator chết SAU capture thì người gọi không nhận kết quả nhưng tiền đã chuyển
  đúng (idempotency-key chống lặp). Choreography + outbox sẽ bền hơn — đánh đổi có chủ đích.
- compliance rule đơn giản (theo ngưỡng) cho minh hoạ.

## Phương án đã cân nhắc
- **Choreography qua Kafka (sự kiện hai chiều):** loại (bước này) — phức tạp hơn cho quy trình đồng bộ
  ngắn; orchestration rõ ràng hơn để minh hoạ + kiểm thử bù trừ.
- **Tách account/money core thành nhiều service rồi Saga phân tán tiền:** loại — rủi ro cao cho invariant
  "sổ luôn cân"; dùng hold của core làm bước phân tán an toàn hơn.
