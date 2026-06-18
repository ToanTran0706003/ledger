# ADR-0006: Transactional outbox + retry cho projection và concurrency

## Trạng thái
Accepted

## Bối cảnh
Phase 3 ("Correctness under Pressure") cần hai bảo đảm:
1. **Không mất projection:** ghi event vào DB và cập nhật read model phải không bị lệch
   kể cả khi process crash giữa chừng.
2. **Đúng dưới đồng thời:** hai lệnh đụng cùng một tài khoản không được tạo số dư sai.

Trước Phase 3, projection chạy *đồng bộ trong cùng transaction* với việc ghi event.
Cách này đơn giản nhưng ghép chặt write side với projection và không có cơ chế phục hồi
nếu projection lỗi sau khi event đã commit.

## Quyết định
**Transactional outbox** (ADR-0003 nay hiện thực hóa):
- Khi append, ghi event vào `events` VÀ một bản vào `outbox` trong **cùng transaction**.
- `OutboxRelay` đọc dòng PENDING, dispatch tới projector, đánh dấu SENT — tất cả trong
  **một transaction** nên "effectively-once": cập nhật read model và đánh dấu SENT
  commit cùng nhau. `FOR UPDATE SKIP LOCKED` cho phép drain song song an toàn.
- **Read-your-writes:** sau khi command commit, handler gọi `relay.drainQuietly()` để
  project ngay → API trả về thấy được thay đổi liền.
- **Lưới an toàn:** `OutboxScheduler` poll định kỳ → nếu drain-sau-commit lỡ thất bại
  (hoặc app crash sau commit), event PENDING vẫn được gửi lại. Tắt trong test để tất định.

**Retry trên xung đột version:** `RetryingTransactionExecutor` chạy mỗi lệnh ghi trong
một transaction (programmatic `TransactionTemplate`); nếu gặp `ConcurrencyConflictException`
(vi phạm UNIQUE aggregate_id+version) thì thử lại với transaction mới — load lại aggregate
ở version mới, validate lại invariant. Dùng TransactionTemplate thay vì @Transactional để
mỗi lần thử là một transaction riêng và tránh bẫy self-invocation.

## Hệ quả
**Được:**
- Không mất event/projection; phục hồi được sau crash (event đã commit luôn được project).
- Projection effectively-once nhờ claim + mark SENT atomic.
- Optimistic concurrency + retry: chứng minh bằng concurrency test (nhiều thread rút cùng
  tài khoản, không bao giờ âm, đúng số giao dịch thành công) và property-based test (jqwik).

**Mất / đánh đổi:**
- Read model trở thành **eventually consistent** về bản chất; ta giữ read-your-writes bằng
  drain-sau-commit, nhưng đây là sự đánh đổi cần hiểu rõ.
- Projector phải an toàn khi chạy lại (idempotent / được bảo vệ bởi claim transaction).
- `SYSTEM_VAULT` là điểm nóng (mọi nạp/rút đụng vault) → nhiều conflict/retry dưới tải;
  concurrency test vì vậy dùng transfer tới các đích khác nhau để cô lập contention. Tối
  ưu vault (vd không khóa optimistic, hoặc sharding) để dành Phase 6.

## Phương án đã cân nhắc
- **Projection đồng bộ trong transaction ghi event** (như Phase 1/2) — loại: ghép chặt,
  không phục hồi được nếu projection lỗi sau commit; không đạt mục tiêu "không mất event".
- **Message broker (Kafka) ngay** — loại: thừa hạ tầng ở monolith (ADR-0003); outbox đã đủ.
- **Pessimistic lock (SELECT FOR UPDATE trên tài khoản)** — loại: optimistic + retry hợp
  với event store append-only hơn và thể hiện đúng tinh thần ES; pessimistic giảm throughput.
