package com.keycode.ilussioness_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.ArrayList;
import java.util.List;

import com.keycode.ilussioness_backend.auth.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Stateless, token-based security for the demo API.
 *
 * <p>{@code POST /api/auth/login} is public. Every other {@code /api/**}
 * endpoint requires a valid {@code Authorization: Bearer <JWT>}. The bounded
 * audit ingestion endpoint ({@code POST /api/audit/events}) remains public
 * because the frontend emits pre-login events and its payload is validated and
 * rate-limited; audit read endpoints are protected.</p>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API calls authenticate with a bearer token. The session cookie is
                // scoped to /api/auth and only exchanged for a token by
                // POST /api/auth/session, whose response a cross-site page cannot
                // read (credentialed CORS is limited to the frontend origins).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        // Authenticated by the HttpOnly session cookie, validated in the controller.
                        .requestMatchers(HttpMethod.POST, "/api/auth/session").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        // OAuth providers redirect the browser here without a bearer token;
                        // the request is authenticated by the signed, short-lived state parameter.
                        .requestMatchers(HttpMethod.GET, "/api/calendar/*/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/audit/events").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, exception) -> unauthorized(response)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> unauthorized(response))
                        .accessDeniedHandler((request, response, exception) -> forbidden(response)));

        return http.build();
    }

    /**
     * Credentialed CORS for {@code /api/auth/**}, so the browser may send and
     * store the session cookie. Only the configured frontend origins are allowed;
     * every other path allows any origin without credentials. The security CORS
     * filter answers preflights from this source, so every path must be covered.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            AuthProperties properties,
            @Value("${app.frontend-url:}") String frontendUrl
    ) {
        List<String> origins = new ArrayList<>(properties.getAllowedOrigins());
        if (!frontendUrl.isBlank()) {
            origins.add(frontendUrl.replaceAll("/+$", ""));
        }

        CorsConfiguration auth = new CorsConfiguration();
        auth.setAllowedOriginPatterns(origins);
        auth.setAllowedMethods(List.of("POST", "OPTIONS"));
        auth.setAllowedHeaders(List.of("*"));
        auth.setAllowCredentials(true);
        auth.setMaxAge(3600L);

        // Same policy as the controllers' @CrossOrigin("*"): bearer-token calls, no cookies.
        CorsConfiguration api = new CorsConfiguration();
        api.addAllowedOrigin("*");
        api.addAllowedMethod("*");
        api.addAllowedHeader("*");
        api.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/auth/**", auth);
        source.registerCorsConfiguration("/**", api);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(AuthProperties properties) {
        return new NimbusJwtEncoder(jwkSource(properties));
    }

    @Bean
    public JwtDecoder jwtDecoder(AuthProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
    }

    private static void forbidden(HttpServletResponse response) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden");
    }

    private static void writeError(HttpServletResponse response, int status, String error)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }

    private static SecretKey secretKey(AuthProperties properties) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.auth.jwt-secret must be configured");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    private static JWKSource<SecurityContext> jwkSource(AuthProperties properties) {
        return new ImmutableSecret<>(secretKey(properties));
    }
}
