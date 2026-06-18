# 03 — Data & Event Store

## 1. Triết lý lưu trữ

Hai loại dữ liệu, hai mục đích trái ngược:

| | Event Store | Read Model |
|--|-------------|------------|
| Mục đích | Nguồn sự thật, audit | Phục vụ đọc nhanh |
| Tính chất | Append-only, bất biến | Có thể dựng lại bất cứ lúc nào |
| Tối ưu cho | Ghi tuần tự, toàn vẹn | Truy vấn, hiển thị |
| Mất đi thì | Thảm họa (mất sự thật) | Không sao (replay lại được) |

> **Quy tắc:** Read model là *dữ liệu phái sinh*. Có thể xóa và dựng lại từ event store bất cứ lúc nào. Event store thì **không bao giờ** được sửa/xóa.

## 2. Schema Event Store (PostgreSQL)

```sql
-- Bảng event: append-only, trái tim hệ thống
CREATE TABLE events (
    global_seq      BIGSERIAL PRIMARY KEY,        -- thứ tự toàn cục
    event_id        UUID NOT NULL UNIQUE,         -- định danh event
    aggregate_id    VARCHAR(64) NOT NULL,         -- thuộc aggregate nào
    aggregate_type  VARCHAR(64) NOT NULL,         -- Account / Transaction
    aggregate_version INT NOT NULL,               -- version trong aggregate
    event_type      VARCHAR(128) NOT NULL,        -- MoneyTransferred...
    event_version   INT NOT NULL DEFAULT 1,       -- schema version của event
    payload         JSONB NOT NULL,               -- nội dung event
    metadata        JSONB,                        -- userId, ip, correlationId...
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- optimistic concurrency: không cho 2 event cùng version trên 1 aggregate
    CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, aggregate_version)
);

CREATE INDEX idx_events_aggregate ON events (aggregate_id, aggregate_version);
CREATE INDEX idx_events_type ON events (event_type);
CREATE INDEX idx_events_occurred ON events (occurred_at);
```

### Vì sao `UNIQUE (aggregate_id, aggregate_version)` là vũ khí chống race condition
Khi load aggregate, ta biết version hiện tại (vd 7). Khi ghi event mới, ta ghi version 8 với kỳ vọng "chưa ai ghi version 8". Nếu hai request đồng thời cùng cố ghi version 8 → DB từ chối cái thứ hai (vi phạm unique) → ta bắt lỗi và báo `ConcurrencyConflict`. **Không cần lock bi quan, DB tự bảo vệ.** Đây là *optimistic concurrency control*.

## 3. Schema Snapshot

```sql
CREATE TABLE snapshots (
    aggregate_id    VARCHAR(64) PRIMARY KEY,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_version INT NOT NULL,    -- snapshot tính đến version này
    state           JSONB NOT NULL,    -- trạng thái đã serialize
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Chiến lược snapshot
- Sau mỗi **N event** (vd N=100) trên một aggregate → ghi snapshot mới (ghi đè cái cũ, vì snapshot là tối ưu, không phải sự thật).
- Khi load aggregate: lấy snapshot (nếu có) → replay chỉ các event *sau* version của snapshot.
- Kết quả: số event phải replay luôn ≤ N → **tốc độ load ≈ hằng số**, không phụ thuộc lịch sử dài bao nhiêu.

## 4. Read Model (ví dụ)

```sql
-- Số dư hiện tại: đọc cực nhanh, không replay
CREATE TABLE rm_account_balance (
    account_id    VARCHAR(64) PRIMARY KEY,
    owner         VARCHAR(128),
    currency      CHAR(3) NOT NULL DEFAULT 'VND',
    balance       NUMERIC(20,2) NOT NULL DEFAULT 0,
    available     NUMERIC(20,2) NOT NULL DEFAULT 0,  -- trừ các hold
    status        VARCHAR(16) NOT NULL,
    last_event_seq BIGINT NOT NULL,                  -- đã projection tới đâu
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Lịch sử giao dịch: phục vụ hiển thị sao kê
CREATE TABLE rm_transaction_history (
    id            BIGSERIAL PRIMARY KEY,
    account_id    VARCHAR(64) NOT NULL,
    tx_id         UUID NOT NULL,
    direction     CHAR(1) NOT NULL,        -- D (debit) / C (credit)
    amount        NUMERIC(20,2) NOT NULL,
    counterparty  VARCHAR(64),
    balance_after NUMERIC(20,2) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    event_type    VARCHAR(128) NOT NULL
);
CREATE INDEX idx_rm_history_account ON rm_transaction_history (account_id, occurred_at DESC);
```

> `last_event_seq` cho phép projection biết đã xử lý tới event nào → hỗ trợ resume, idempotent projection, và phát hiện tụt hậu (lag).

## 5. Đảm bảo không mất event: Transactional Outbox

Vấn đề: ghi event vào DB *và* phát đi cho projector/broker phải nguyên tử. Nếu ghi DB xong mà phát thất bại → projector không biết → read model lệch.

Giải pháp **Outbox**: trong cùng một transaction DB, ghi event vào bảng `events` *và* ghi một bản vào `outbox`. Một tiến trình riêng (relay) đọc outbox, dispatch tới projector (in-process ở monolith, Kafka ở distributed), rồi đánh dấu đã gửi. Vì cùng transaction nên không bao giờ mất.

```sql
CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL,
    payload      JSONB NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/SENT
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at      TIMESTAMPTZ
);
```

## 6. Event Versioning (schema event thay đổi theo thời gian)

Event sống mãi, nhưng cấu trúc payload có thể đổi. Chiến lược:
- **Cộng dồn, không phá vỡ:** chỉ thêm field optional, không xóa/đổi nghĩa field cũ.
- **Upcasting:** khi đọc event version cũ, một "upcaster" chuyển nó sang dạng mới trong bộ nhớ trước khi xử lý. `event_version` trong bảng cho biết cần upcast hay không.
- Không bao giờ sửa event cũ trong DB.

## 7. Idempotency Store

```sql
CREATE TABLE idempotency_keys (
    idem_key      VARCHAR(128) PRIMARY KEY,
    request_hash  VARCHAR(64) NOT NULL,    -- hash của payload để phát hiện reuse sai
    response_body JSONB,                   -- kết quả đã trả, để trả lại nếu gọi lại
    status        VARCHAR(16) NOT NULL,    -- IN_PROGRESS / COMPLETED
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ
);
```

Chi tiết cách dùng ở `04-security-and-anti-cheat.md` mục Idempotency.

## 8. Chiến lược lưu trữ dài hạn (archiving)
- Event "nóng" (gần đây) ở PostgreSQL chính.
- Event "lạnh" (cũ > X tháng) có thể chuyển sang bảng partition theo thời gian, hoặc xuất sang object storage (S3-compatible) dạng nén.
- Read model + snapshot đảm bảo vận hành hằng ngày không cần đụng event lạnh.
- Với banking: **giữ toàn bộ lịch sử** (yêu cầu audit/pháp lý) — archiving là để *rẻ hóa* lưu trữ, không phải để xóa.

## 9. Bước kế tiếp
Đọc `04-security-and-anti-cheat.md` — phần tập trung giá trị kỹ thuật nhất của dự án.
