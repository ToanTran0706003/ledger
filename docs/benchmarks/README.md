# Benchmarks

> Hiệu năng đo thật, không khoe suông (xem `../05-performance-and-scaling.md` mục 6).

## Mục tiêu (từ doc 05)

| Chỉ số | Mục tiêu MVP | Mục tiêu Flagship |
|--------|--------------|-------------------|
| Đọc số dư (p99) | < 50ms | < 20ms |
| Ghi giao dịch (p99) | < 200ms | < 100ms |

## Kết quả — single-client baseline

- **Ngày:** 2026-06-18
- **Máy:** Windows 11, JDK 21, PostgreSQL 18 local (cùng máy)
- **Cách đo:** 1 client tuần tự, 200 request mỗi loại sau khi warm-up (xem `ops/loadtest/`).
- **Lưu ý:** đây là *latency baseline 1 client*, chưa phải throughput dưới tải đồng thời —
  dùng k6 (bên dưới) để đo concurrency/throughput.

| Thao tác | p50 | p95 | p99 | max | Mục tiêu MVP | Đạt? |
|----------|-----|-----|-----|-----|--------------|------|
| Ghi (deposit) | 7.9ms | 10.8ms | 14.6ms | 21.7ms | <200ms | ✅ (cả flagship <100ms) |
| Đọc (balance) | 3.1ms | 3.9ms | 4.3ms | 17.2ms | <50ms | ✅ (cả flagship <20ms) |

Đọc nhanh vì phục vụ từ read model (`rm_account_balance`, SELECT 1 dòng) — không replay.
Ghi gồm: load aggregate (snapshot + ≤N event) → validate invariant → append (event+outbox)
trong transaction → drain projection.

## Metrics (Prometheus) — `/actuator/prometheus`

| Metric | Ý nghĩa |
|--------|---------|
| `ledger_command_duration_seconds` | Độ trễ lệnh ghi (tag `operation`) |
| `ledger_transactions_total` | Throughput theo `type` (DEPOSIT/WITHDRAWAL/TRANSFER) |
| `ledger_concurrency_conflicts_total` | Tỉ lệ xung đột optimistic concurrency (số lần retry) |
| `ledger_outbox_pending` | **Projection lag** — số event chưa project (chỉ số sống còn của CQRS) |

## Chạy load test đồng thời (k6)

Cần [k6](https://k6.io). Backend chạy ở `http://localhost:8080`.

```bash
k6 run ops/loadtest/deposit-load.js
```

Script tự đăng ký user, mở tài khoản, rồi bắn deposit đồng thời (nhiều VU), in p95/p99.
Ghi lại kết quả vào bảng trên khi chạy trên môi trường mục tiêu.

## Điểm nóng đã biết

`SYSTEM_VAULT` bị đụng ở mọi nạp/rút → dưới tải đồng thời cao sẽ tăng
`ledger_concurrency_conflicts_total`. Hướng tối ưu (doc 05 mục 5): shard vault hoặc
kỹ thuật conflict-free cho riêng vault — để dành khi đo thấy nghẽn thật.
