package com.ledger.account.fx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Tra tỉ giá quy đổi từ cấu hình; ném nếu chưa cấu hình cặp tiền tệ (không quy đổi mò). */
@Service
public class FxRateService {

    private final FxProperties properties;

    public FxRateService(FxProperties properties) {
        this.properties = properties;
    }

    public BigDecimal rate(String from, String to) {
        Map<String, BigDecimal> row = properties.rates().get(from);
        BigDecimal rate = row == null ? null : row.get(to);
        if (rate == null) {
            throw new IllegalArgumentException("Chưa cấu hình tỉ giá quy đổi " + from + " -> " + to);
        }
        return rate;
    }
}
