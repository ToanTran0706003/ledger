package com.ledger.iam;

import com.ledger.iam.domain.UserAccount;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Phát hành và xác thực JWT (HS256). Access token ngắn hạn, refresh token dài hạn. */
@Service
public class JwtService {

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(
            JwtEncoder encoder,
            JwtDecoder decoder,
            @Value("${ledger.security.jwt.access-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${ledger.security.jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String issueAccessToken(UserAccount user) {
        return issue(user, TYPE_ACCESS, Duration.ofMinutes(accessTtlMinutes));
    }

    public String issueRefreshToken(UserAccount user) {
        return issue(user, TYPE_REFRESH, Duration.ofDays(refreshTtlDays));
    }

    /** Xác thực refresh token và trả claims; ném nếu không hợp lệ/hết hạn/không phải refresh. */
    public Jwt verifyRefreshToken(String token) {
        Jwt jwt = decoder.decode(token);
        if (!TYPE_REFRESH.equals(jwt.getClaimAsString("typ"))) {
            throw new IllegalArgumentException("Token không phải refresh token");
        }
        return jwt;
    }

    private String issue(UserAccount user, String type, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("username", user.getUsername())
                .claim("roles", List.of(user.getRole().name()))
                .claim("typ", type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
