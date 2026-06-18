# ADR-0010: Observability & Performance

## Trạng thái
Accepted

## Bối cảnh
Muốn nói về hiệu năng một cách nghiêm túc thì phải *đo* được (doc 05 mục 6, 8): cần
metrics, log truy vết được, và benchmark thật thay vì tuyên bố suông.

## Quyết định
**Metrics (Micrometer + Prometheus):** phơi `/actuator/prometheus`. Bốn nhóm chỉ số theo
doc 05: độ trễ lệnh (`ledger_command_duration`, timer theo operation), throughput
(`ledger_transactions_total` theo type), tỉ lệ xung đột (`ledger_concurrency_conflicts_total`,
tăng trong RetryingTransactionExecutor), và **projection lag** (`ledger_outbox_pending`,
gauge đếm outbox PENDING — chỉ số sống còn của CQRS).

**Logs có cấu trúc:** profile `prod` bật `logging.structured.format.console=ecs` (JSON ECS),
kèm `correlationId`/`userId` qua MDC (đã có từ Phase 4/5). Dev giữ log text dễ đọc.

**Benchmark:** đo thật và ghi vào `docs/benchmarks/`. Baseline 1 client trên máy dev:
ghi p99 ≈ 14.6ms, đọc p99 ≈ 4.3ms — vượt mục tiêu MVP/flagship. Kèm script k6
(`ops/loadtest/`) để đo concurrency/throughput trên môi trường mục tiêu.

**Tối ưu theo đo:** thêm index biểu thức `idx_events_txid` trên `payload->>'txId'`
(MoneyPosted) để reversal không quét tuần tự bảng events (V8).

## Hệ quả
**Được:**
- Có số liệu thật để kể chuyện hiệu năng; projection lag quan sát được.
- correlationId/userId xuyên suốt log + event metadata → truy vết một command end-to-end.
- Reversal tra cứu theo index thay vì full scan.

**Mất / đánh đổi:**
- `/actuator/prometheus` để công khai cho demo — ở prod nên đặt sau cổng/mạng quản trị riêng.
- Gauge projection lag query `count(*)` outbox mỗi lần scrape — rẻ ở quy mô hiện tại.
- Chưa làm: tracing OpenTelemetry, dashboard Grafana (cấu hình sẵn để thêm sau), tối ưu
  điểm nóng vault (chỉ làm khi đo thấy nghẽn thật — doc 05 mục 5).

## Phương án đã cân nhắc
- **Không metrics, chỉ log** — loại: không định lượng được hiệu năng/lag.
- **Tối ưu vault ngay (shard)** — hoãn: chưa đo thấy nghẽn; thêm phức tạp sớm là phản
  nguyên tắc (doc 05 mục 7). Đã có metric conflict để biết *khi nào* cần.
- **Đo bằng tuyên bố/cảm tính** — loại: doc yêu cầu số liệu thật.
