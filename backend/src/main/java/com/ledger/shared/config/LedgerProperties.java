package com.ledger.shared.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình dưới tiền tố `ledger`. seedAmount là lượng tiền "khai sinh" nạp vào
 * SYSTEM_VAULT lúc khởi tạo — cũng là hằng số mà integrity check đối chiếu.
 */
@ConfigurationProperties("ledger")
public record LedgerProperties(Vault vault) {

    public record Vault(BigDecimal seedAmount) {}
}
