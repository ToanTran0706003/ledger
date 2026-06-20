package com.ledger.audit.web;

import java.util.List;

public record IntegrityResponse(
        List<CurrencyTotal> perCurrency,
        boolean balanced
) {
}
