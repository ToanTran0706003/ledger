-- Transactional outbox: ghi cùng transaction với events để không bao giờ mất event.
-- Một relay đọc các dòng PENDING, dispatch tới projector, rồi đánh dấu SENT (ADR-0006).
CREATE TABLE outbox (
    id          BIGSERIAL PRIMARY KEY,
    event_id    UUID NOT NULL,
    event_type  VARCHAR(128) NOT NULL,
    payload     JSONB NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',   -- PENDING / SENT
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);

-- Index một phần: relay chỉ quét các dòng còn PENDING.
CREATE INDEX idx_outbox_pending ON outbox (id) WHERE status = 'PENDING';
