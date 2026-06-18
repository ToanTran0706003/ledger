package com.ledger.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * Đảm bảo một lệnh ghi (gắn Idempotency-Key) chỉ tạo hiệu lực một lần (04-security mục 5).
 * - Key mới: ghi IN_PROGRESS, chạy action, lưu response + COMPLETED.
 * - Key đã COMPLETED: trả lại response cũ, KHÔNG chạy lại.
 * - Key đang IN_PROGRESS: 409 (đang xử lý) — tránh chạy song song.
 * - Cùng key, payload khác: 422.
 *
 * Các thao tác idempotency chạy ngoài transaction nghiệp vụ (autocommit) nên dòng
 * IN_PROGRESS hiện ngay với request trùng. action tự quản transaction riêng.
 */
@Service
public class IdempotencyService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public IdempotencyService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public static String hashOf(String... parts) {
        return DigestUtils.md5DigestAsHex(String.join("|", parts).getBytes(StandardCharsets.UTF_8));
    }

    public <T> T execute(String key, String requestHash, Class<T> responseType, Supplier<T> action) {
        if (!claim(key, requestHash)) {
            return replayExisting(key, requestHash, responseType);
        }

        T result;
        try {
            result = action.get();
        } catch (RuntimeException ex) {
            // action thất bại (vd không đủ số dư) -> nhả key để client có thể thử lại.
            jdbc.update("DELETE FROM idempotency_keys WHERE idem_key = ? AND status = 'IN_PROGRESS'", key);
            throw ex;
        }

        jdbc.update(
                "UPDATE idempotency_keys SET status = 'COMPLETED', response_body = ?::jsonb WHERE idem_key = ?",
                serialize(result), key);
        return result;
    }

    /** @return true nếu giành được key (ghi IN_PROGRESS thành công). */
    private boolean claim(String key, String requestHash) {
        try {
            jdbc.update(
                    "INSERT INTO idempotency_keys (idem_key, request_hash, status) VALUES (?, ?, 'IN_PROGRESS')",
                    key, requestHash);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private <T> T replayExisting(String key, String requestHash, Class<T> responseType) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT request_hash, status, response_body FROM idempotency_keys WHERE idem_key = ?", key);
        if (rows.isEmpty()) {
            // Hiếm gặp (bị xóa giữa chừng); coi như đang chạy.
            throw new IdempotencyInProgressException(key);
        }
        Map<String, Object> row = rows.getFirst();
        if (!requestHash.equals(row.get("request_hash"))) {
            throw new IdempotencyConflictException(key);
        }
        if (!"COMPLETED".equals(row.get("status"))) {
            throw new IdempotencyInProgressException(key);
        }
        return deserialize(String.valueOf(row.get("response_body")), responseType);
    }

    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được response idempotency", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Không deserialize được response idempotency", e);
        }
    }
}
