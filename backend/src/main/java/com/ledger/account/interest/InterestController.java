package com.ledger.account.interest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tính lãi tiết kiệm — thao tác của ngân hàng (ADMIN, theo /admin/** trong SecurityConfig).
 * Thực tế nên chạy theo lịch; endpoint cho phép kích hoạt thủ công với mốc thời gian tuỳ chọn.
 */
@RestController
@RequestMapping("/admin/accounts")
public class InterestController {

    private final InterestService interestService;

    public InterestController(InterestService interestService) {
        this.interestService = interestService;
    }

    public record AccrueInterestResponse(String accountId, BigDecimal interest) {}

    @PostMapping("/{accountId}/accrue-interest")
    public AccrueInterestResponse accrue(@PathVariable String accountId, @RequestParam(required = false) String asOf) {
        Instant at = parseAsOf(asOf);
        return new AccrueInterestResponse(accountId, interestService.accrue(accountId, at));
    }

    private static Instant parseAsOf(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("asOf phải là ISO-8601");
        }
    }
}
