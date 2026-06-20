package com.ledger.iam;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * TOTP (RFC 6238) cho xác thực hai lớp: mã 6 chữ số, bước 30s, HMAC-SHA1 — tương thích Google
 * Authenticator/Authy. Tự cài, không thư viện ngoài. Cho phép lệch ±1 bước để bù sai lệch đồng hồ.
 */
@Service
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int DIGITS = 6;
    private static final long STEP_SECONDS = 30;
    private static final int WINDOW = 1;
    private static final String ISSUER = "Ledger";

    private final SecureRandom random = new SecureRandom();

    /** Sinh bí mật TOTP 160-bit (khuyến nghị RFC), mã hoá Base32 để nhập vào app xác thực. */
    public String generateSecret() {
        byte[] buf = new byte[20];
        random.nextBytes(buf);
        return base32Encode(buf);
    }

    /** Kiểm mã người dùng nhập so với bước hiện tại và ±1 bước (bù lệch đồng hồ). */
    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        long counter = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (long i = -WINDOW; i <= WINDOW; i++) {
            if (codeAt(key, counter + i).equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** URI otpauth:// để app xác thực quét QR (hoặc nhập tay bí mật). */
    public String otpauthUri(String secret, String account) {
        return "otpauth://totp/" + enc(ISSUER) + ":" + enc(account)
                + "?secret=" + secret + "&issuer=" + enc(ISSUER) + "&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    // HOTP (RFC 4226) của counter -> rút DIGITS chữ số bằng dynamic truncation.
    String codeAt(byte[] key, long counter) {
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] h = hmacSha1(key, data);
        int offset = h[h.length - 1] & 0x0f;
        int bin = ((h[offset] & 0x7f) << 24)
                | ((h[offset + 1] & 0xff) << 16)
                | ((h[offset + 2] & 0xff) << 8)
                | (h[offset + 3] & 0xff);
        int otp = bin % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", otp);
    }

    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tính được HMAC-SHA1", e);
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(BASE32.charAt((buffer >> bits) & 0x1f));
            }
        }
        if (bits > 0) {
            sb.append(BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String s) {
        String clean = s.trim().replace("=", "").toUpperCase();
        int buffer = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int val = BASE32.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Ký tự Base32 không hợp lệ");
            }
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xff);
            }
        }
        return out.toByteArray();
    }
}
