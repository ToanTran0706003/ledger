package com.ledger.account.hold;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Body đặt giữ: số tiền dương và thời hạn (giây) trước khi tự nhả. */
public record PlaceHoldRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull @Positive Long ttlSeconds) {
}
