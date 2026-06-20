package com.ledger.account.domain;

import java.math.BigDecimal;

/** Ném ra khi một lệnh ghi nợ làm tổng ghi nợ trong ngày của tài khoản vượt hạn mức. */
public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException(
            String accountId, BigDecimal limit, BigDecimal alreadyToday, BigDecimal requested) {
        super("Tài khoản %s vượt hạn mức ngày %s: đã ghi nợ %s, lệnh này %s"
                .formatted(accountId, limit.toPlainString(), alreadyToday.toPlainString(), requested.toPlainString()));
    }
}
