package com.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledger.account.api.AmountRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Ràng buộc số tiền (audit #6): chặn số vượt biên NUMERIC(20,2) và scale lẻ ngay ở tầng validation
 * (-> 400) thay vì để lọt xuống DB gây 500 + lệch read model so với event store.
 */
class AmountRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rejects_too_many_decimals() {
        assertThat(validator.validate(new AmountRequest(new BigDecimal("100.999")))).isNotEmpty();
    }

    @Test
    void rejects_amount_exceeding_numeric_bound() {
        assertThat(validator.validate(new AmountRequest(new BigDecimal("1E19")))).isNotEmpty();
    }

    @Test
    void accepts_normal_amount() {
        assertThat(validator.validate(new AmountRequest(new BigDecimal("100.50")))).isEmpty();
    }
}
