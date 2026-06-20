package com.ledger.iam;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật: API stateless, xác thực bằng JWT (HS256). Endpoint /auth công khai;
 * /admin và reverse cần ADMIN; /audit cần ADMIN hoặc AUDITOR; còn lại cần đăng nhập
 * (ownership check thực thi ở tầng controller). Xem ADR-0009.
 */
@Configuration
public class SecurityConfig {

    private final String jwtSecret;
    private final String corsAllowedOrigins;

    public SecurityConfig(
            @Value("${ledger.security.jwt.secret}") String jwtSecret,
            @Value("${ledger.cors.allowed-origins:*}") String corsAllowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        // Prometheus scrape công khai cho demo; ở prod nên đặt sau cổng/mạng quản trị riêng.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/*/reverse").hasRole("ADMIN")
                        // Integrity là chỉ số minh bạch "sổ luôn cân" — cho mọi user đã đăng nhập xem.
                        .requestMatchers("/audit/integrity").authenticated()
                        .requestMatchers("/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .headers(h -> h
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true))
                        // Phòng thủ chiều sâu cho phần frontend (audit #7): chặn nguồn ngoài + nhúng iframe.
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; base-uri 'self'")));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Dev mặc định '*'; prod đặt LEDGER_CORS_ORIGINS = danh sách domain frontend (phẩy) -> chỉ
        // các domain đó được gọi API (audit #4). '*' dùng allowedOriginPatterns, domain cụ thể dùng
        // allowedOrigins (chặt hơn).
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        CorsConfiguration config = new CorsConfiguration();
        if (origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            return roles.stream().map(role -> (org.springframework.security.core.GrantedAuthority)
                    new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        return converter;
    }
}
