package com.ledger.audit.web;

import java.math.BigDecimal;

public record CurrencyTotal(
        String currency,
        BigDecimal total
) {
}
