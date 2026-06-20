package com.ledger.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** TOTP (RFC 6238): kiểm bằng vector chuẩn của RFC + round-trip Base32 + verify chấp nhận/từ chối. */
class TotpServiceTest {

    private final TotpService totp = new TotpService();

    @Test
    void rfc6238_sha1_test_vectors() {
        // Seed chuẩn của RFC 6238 (Appendix B): "12345678901234567890" (20 byte ASCII).
        byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        // T=59s  -> counter 1        -> TOTP 94287082 -> 6 chữ số 287082
        assertThat(totp.codeAt(key, 1L)).isEqualTo("287082");
        // T=1111111109s -> counter 37037036 -> TOTP 07081804 -> 6 chữ số 081804
        assertThat(totp.codeAt(key, 37037036L)).isEqualTo("081804");
    }

    @Test
    void base32_round_trip() {
        byte[] data = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertThat(TotpService.base32Decode(TotpService.base32Encode(data))).isEqualTo(data);
    }

    @Test
    void verify_accepts_current_code_and_rejects_wrong() {
        String secret = totp.generateSecret();
        long counter = Instant.now().getEpochSecond() / 30;
        String current = totp.codeAt(TotpService.base32Decode(secret), counter);

        assertThat(totp.verify(secret, current)).isTrue();
        assertThat(totp.verify(secret, "000000")).isFalse();
        assertThat(totp.verify(secret, "notacode")).isFalse();
    }
}
