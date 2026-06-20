-- Nâng hash-chain từ SHA-256 (tamper-EVIDENT) lên HMAC-SHA256 với KHOÁ BÍ MẬT ngoài DB
-- (tamper-PROOF): attacker có quyền ghi DB nhưng KHÔNG có khoá thì không tự tính lại chuỗi hash
-- hợp lệ (xem ADR-0014 hướng nâng cấp, thực thi ở ADR-0021). Khoá lấy từ Flyway placeholder
-- ${hashchainKey} (= ledger.security.hashchain.key của app; prod đặt LEDGER_HASHCHAIN_KEY).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Re-backfill TOÀN BỘ chuỗi với HMAC. Biểu thức PHẢI khớp y hệt lúc append (JdbcEventStore) và
-- lúc verify (HashChainVerifier).
DO $$
DECLARE
  r RECORD;
  last_hash TEXT;
  genesis CONSTANT TEXT := repeat('0', 64);
  last_agg TEXT := NULL;
  k CONSTANT TEXT := '${hashchainKey}';
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
        hash = encode(hmac(convert_to(
                 last_hash || r.aggregate_id || ':' || r.aggregate_version::text || ':'
                 || r.event_type || ':' || r.payload::text || ':' || COALESCE(r.metadata::text, ''),
                 'UTF8'), convert_to(k, 'UTF8'), 'sha256'), 'hex')
    WHERE global_seq = r.global_seq;
    SELECT hash INTO last_hash FROM events WHERE global_seq = r.global_seq;
    last_agg := r.aggregate_id;
  END LOOP;
END $$;
