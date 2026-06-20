package com.ledger.account.fx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tỉ giá quy đổi do hệ thống cấu hình (KHÔNG để client tự đặt — tránh tự "in tiền").
 * Dạng lồng: {@code ledger.fx.rates.<FROM>.<TO> = rate} (1 đơn vị FROM = rate đơn vị TO).
 */
@ConfigurationProperties("ledger.fx")
public record FxProperties(Map<String, Map<String, BigDecimal>> rates) {

    public FxProperties {
        rates = rates == null ? Map.of() : rates;
    }
}
