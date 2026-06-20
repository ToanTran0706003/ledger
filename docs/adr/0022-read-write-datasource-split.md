# ADR-0022: Tách read/write datasource (CQRS, Phase 9)

## Trạng thái
Accepted

## Bối cảnh
Phase 9 (phân tán) đa phần cần hạ tầng chạy được (Kafka broker, nhiều service, orchestration) — máy
hiện tại **Docker/WSL hỏng** nên Kafka / tách microservice / Saga sẽ là code không chạy được (dead
code, trái nguyên tắc "không code đầu cơ"). Phần Phase 9 **làm được tử tế không cần Docker** là tách
read/write datasource để minh hoạ CQRS ở tầng lưu trữ.

## Quyết định
- **Hai datasource:** `writeDataSource` (primary) cho đường GHI và mọi **đọc-trong-giao-dịch**
  (event store, projector, ownership/hạn mức/fraud — cần đọc-thấy-ghi); `readDataSource` cho **đọc
  nặng kiểm toán/báo cáo** (`IntegrityService` quét tổng số dư, `HashChainVerifier` quét toàn bảng
  events). Inject qua `@Qualifier("readJdbcTemplate")`.
- **Replica-ready:** prod đặt `LEDGER_READ_DATASOURCE_URL` = một **read replica**; projector vẫn ghi
  read model vào primary, replica nhận qua replication. Trên một máy dev mặc định trỏ cùng DB ghi nên
  không cần hạ tầng. Flyway/JPA dùng primary (@Primary).
- **Vì sao chỉ tách đường đọc kiểm toán, không tách toàn bộ đọc:** read model còn được dùng TRONG
  giao dịch (ownership check, hạn mức, fraud). Đưa các đọc đó sang replica sẽ vỡ **read-your-writes**
  (lag replica → mở tài khoản rồi nạp ngay có thể không thấy). Nên chỉ offload các truy vấn đọc-nặng,
  không-trong-giao-dịch — đây cũng là cách hệ thống thật giảm tải primary.
- **Pool đọc nhỏ** (`maximum-pool-size` 4 prod / 2 test): đọc kiểm toán ít đồng thời; tránh nhân đôi
  kết nối tới DB (test nhiều application-context cache → giữ tổng kết nối dưới `max_connections`).

## Hệ quả
**Được:** minh hoạ CQRS read/write split thật ở tầng datasource, sẵn sàng trỏ read replica bằng một
biến môi trường — giảm tải đọc-nặng khỏi primary. 105 test xanh (test integrity/hash-chain nay đi
qua read datasource).
**Mất / giới hạn:**
- Trên một máy dev là "logic split" (hai pool, cùng DB) cho tới khi prod cấu hình replica thật.
- **Kafka event backbone, tách microservice, Saga liên service: HOÃN — cần Docker** (đang hỏng). Sẽ
  làm khi môi trường container hoạt động; viết bây giờ chỉ ra code không chạy được.

## Phương án đã cân nhắc
- **Hai database vật lý tách hẳn (read model ở DB riêng, không replica):** loại — projector phải ghi
  chéo DB, mọi test phải truncate trên 2 DB, tách migration thành 2 bộ (vỡ lịch sử Flyway DB sẵn có);
  churn + rủi ro lớn cho một tách "nhân tạo" trên một máy. Replica-routing đạt mục tiêu an toàn hơn.
- **Tách toàn bộ đường đọc sang replica:** loại — vỡ read-your-writes ở đường lệnh (xem trên).
