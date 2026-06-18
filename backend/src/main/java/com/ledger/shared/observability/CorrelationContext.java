package com.ledger.shared.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Giữ correlationId của request hiện tại (ThreadLocal) để gắn vào metadata mỗi event,
 * phục vụ truy vết audit (xem 04-security mục 7). userId/ip sẽ thêm ở Phase 5 (auth).
 */
@Component
public class CorrelationContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private final ObjectMapper mapper;

    public CorrelationContext(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    public void clear() {
        CORRELATION_ID.remove();
    }

    /** Metadata JSON cho event hiện tại (correlationId + userId), hoặc null nếu không có. */
    public String currentMetadataJson() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        String correlationId = CORRELATION_ID.get();
        if (correlationId != null) {
            metadata.put("correlationId", correlationId);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            metadata.put("userId", auth.getName());
        }

        if (metadata.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return null; // metadata là phụ trợ — không để hỏng việc ghi event
        }
    }
}
