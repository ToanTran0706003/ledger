-- Hash-chain chống giả mạo cho event store (sổ cái bất biến, kiểm toán được).
-- Mỗi event mang hash = SHA-256 của (hash event trước trong CÙNG aggregate + nội dung của nó).
-- Chuỗi theo từng aggregate: tận dụng optimistic concurrency đã serialize ghi theo aggregate,
-- nên KHÔNG cần khoá toàn cục (giữ nguyên khả năng ghi song song giữa các aggregate).
-- Dùng sha256() built-in (PostgreSQL 11+) nên không cần extension pgcrypto.
ALTER TABLE events ADD COLUMN prev_hash VARCHAR(64);
ALTER TABLE events ADD COLUMN hash VARCHAR(64);

-- Backfill các event đã có (nếu DB không trống): dựng chuỗi theo thứ tự aggregate + version.
-- Biểu thức hash ở đây PHẢI khớp y hệt lúc append (JdbcEventStore) và lúc verify (HashChainVerifier).
DO $$
DECLARE
  r RECORD;
  last_hash TEXT;
  genesis CONSTANT TEXT := repeat('0', 64);
  last_agg TEXT := NULL;
BEGIN
  FOR r IN
    SELECT global_seq, aggregate_id, aggregate_version, event_type, payload, metadata
    FROM events
    ORDER BY aggregate_id, aggregate_version
  LOOP
    IF last_agg IS DISTINCT FROM r.aggregate_id THEN
      last_hash := genesis;
    END IF;
    UPDATE events
    SET prev_hash = last_hash,
        hash = encode(sha256(convert_to(
                 last_hash || r.aggregate_id || ':' || r.aggregate_version::text || ':'
                 || r.event_type || ':' || r.payload::text || ':' || COALESCE(r.metadata::text, ''),
                 'UTF8')), 'hex')
    WHERE global_seq = r.global_seq;
    SELECT hash INTO last_hash FROM events WHERE global_seq = r.global_seq;
    last_agg := r.aggregate_id;
  END LOOP;
END $$;

ALTER TABLE events ALTER COLUMN prev_hash SET NOT NULL;
ALTER TABLE events ALTER COLUMN hash SET NOT NULL;
