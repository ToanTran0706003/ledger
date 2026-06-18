-- Event store: bảng append-only, nguồn sự thật duy nhất của hệ thống.
-- Không bao giờ UPDATE/DELETE các dòng ở đây (xem ADR-0002).
CREATE TABLE events (
    global_seq        BIGSERIAL PRIMARY KEY,        -- thứ tự toàn cục, dùng cho projection/rebuild
    event_id          UUID NOT NULL UNIQUE,         -- định danh duy nhất của event
    aggregate_id      VARCHAR(64) NOT NULL,         -- event thuộc aggregate nào
    aggregate_type    VARCHAR(64) NOT NULL,         -- Account / Transaction / ...
    aggregate_version INT NOT NULL,                 -- version của event trong aggregate (1,2,3...)
    event_type        VARCHAR(128) NOT NULL,        -- tên logic của event (AccountOpened...)
    event_version     INT NOT NULL DEFAULT 1,       -- schema version của event (cho upcasting sau này)
    payload           JSONB NOT NULL,               -- nội dung event
    metadata          JSONB,                        -- userId, ip, correlationId... (điền từ Phase 4/5)
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Optimistic concurrency: cấm 2 event cùng version trên 1 aggregate.
    -- Đây là lá chắn chống race condition (xem 04-security mục 4).
    CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, aggregate_version)
);

CREATE INDEX idx_events_aggregate ON events (aggregate_id, aggregate_version);
CREATE INDEX idx_events_type ON events (event_type);
CREATE INDEX idx_events_occurred ON events (occurred_at);
