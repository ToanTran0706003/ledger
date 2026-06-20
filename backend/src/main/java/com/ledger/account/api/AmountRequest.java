package com.ledger.account.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Body cho nạp/rút: số tiền dương, tối đa 16 chữ số nguyên + 2 thập phân (khớp NUMERIC(20,2)). */
public record AmountRequest(
        @NotNull @Positive @Digits(integer = 16, fraction = 2) BigDecimal amount) {
}
