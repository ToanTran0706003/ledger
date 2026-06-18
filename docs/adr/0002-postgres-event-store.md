# ADR-0002: PostgreSQL làm Event Store thay vì EventStoreDB

## Trạng thái
Accepted

## Bối cảnh
Event Sourcing cần một kho lưu event append-only làm nguồn sự thật duy nhất. Lựa chọn:
dùng một event store chuyên dụng (EventStoreDB) hay xây trên PostgreSQL?

Ràng buộc:
- Budget $0, hạ tầng phải đơn giản để một người vận hành.
- Cần optimistic concurrency mạnh và truy vấn linh hoạt (time-travel, audit, integrity).
- Read model cũng dùng PostgreSQL — giữ một loại hạ tầng giảm chi phí học và vận hành.

## Quyết định
Dùng **PostgreSQL** làm event store: một bảng `events` append-only, payload lưu dạng
`JSONB`, optimistic concurrency bằng ràng buộc `UNIQUE (aggregate_id, aggregate_version)`.
Truy cập event store bằng **JDBC thuần** (kiểm soát chặt), read model dùng Spring Data
JPA. Migration quản lý bằng Flyway.

## Hệ quả
**Được:**
- Một loại DB cho cả write và read → ít hạ tầng, ít thứ phải học/vận hành.
- `UNIQUE (aggregate_id, aggregate_version)` cho optimistic concurrency "miễn phí":
  hai request cùng ghi một version → DB từ chối cái thứ hai → bắt thành
  `ConcurrencyConflict`, không cần pessimistic lock.
- `JSONB` + SQL cho phép truy vấn audit/time-travel linh hoạt.
- Transaction ACID giúp làm transactional outbox trong cùng một transaction (xem ADR-0003).

**Mất / đánh đổi:**
- Không có sẵn subscription/projection như EventStoreDB — ta tự dựng projector + outbox.
- Phải tự lo append tuần tự hiệu năng cao; chấp nhận được ở quy mô portfolio, có thể
  partition theo thời gian khi cần.

## Phương án đã cân nhắc
- **EventStoreDB** — loại: thêm một hạ tầng chuyên dụng phải học và vận hành; lợi ích
  (subscription, projection sẵn) chưa cần ở giai đoạn này và làm phức tạp deploy $0.
- **Lưu event trong một bảng nhưng dùng JPA cho cả event store** — loại: JPA che mất
  kiểm soát SQL/append; event store cần kiểm soát chặt nên dùng JDBC thuần.
