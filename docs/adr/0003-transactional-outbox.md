# ADR-0003: Transactional Outbox thay vì Message Broker ở giai đoạn đầu

## Trạng thái
Accepted

## Bối cảnh
Sau khi append event, ta phải phát event tới projector (cập nhật read model) và sau này
tới các service khác. Rủi ro kinh điển: ghi event vào DB thành công nhưng phát đi thất
bại (hoặc ngược lại) → read model lệch, mất event. Cần đảm bảo "ghi" và "phát" nguyên tử.

Ràng buộc:
- Đang ở Modular Monolith (ADR-0001), dispatch tới projector là in-process.
- Budget $0, không muốn vận hành Kafka/broker khi chưa thật sự cần.

## Quyết định
Dùng **Transactional Outbox**: trong cùng một transaction DB với việc append vào bảng
`events`, ghi thêm một bản vào bảng `outbox`. Một relay riêng đọc `outbox`, dispatch tới
projector (in-process ở monolith), rồi đánh dấu `SENT`. Vì cùng một transaction nên
không bao giờ mất event hay phát mồ côi.

Message broker (Kafka) được hoãn tới Phase 9: khi lên distributed, relay sẽ dispatch ra
Kafka thay vì in-process — phần ghi outbox không đổi.

## Hệ quả
**Được:**
- Đảm bảo at-least-once delivery mà không cần broker; tận dụng ACID của PostgreSQL.
- Không mất event kể cả khi process crash giữa chừng (relay sẽ gửi lại bản PENDING).
- Đường nâng cấp lên Kafka rõ ràng, chỉ đổi điểm dispatch.

**Mất / đánh đổi:**
- Projector phải **idempotent** (có thể nhận lại event đã xử lý) — dùng `last_event_seq`
  trong read model để bỏ qua event đã áp dụng.
- Thêm một bảng `outbox` và một tiến trình relay phải vận hành/giám sát.
- Là at-least-once chứ không exactly-once; thiết kế consumer phải chịu được trùng lặp.

## Phương án đã cân nhắc
- **Kafka/RabbitMQ ngay từ đầu** — loại: thêm hạ tầng nặng khi dispatch còn in-process;
  không giải quyết được vấn đề nguyên tử "ghi DB + phát" nếu thiếu outbox, mà lại tốn
  chi phí vận hành.
- **Gọi projector trực tiếp trong transaction ghi event** — loại: nếu projector lỗi sẽ
  kéo rollback cả việc append event (hoặc ngược lại nếu để ngoài transaction sẽ mất
  event); ghép chặt write side với projection.
- **Dual write (ghi DB rồi gọi broker, không outbox)** — loại: chính là lỗi kinh điển
  mất nhất quán khi một trong hai bước thất bại.
