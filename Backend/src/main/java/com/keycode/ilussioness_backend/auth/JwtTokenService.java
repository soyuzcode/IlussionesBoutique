package com.keycode.ilussioness_backend.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Issues short-lived HS256 JWTs for the demo account.
 *
 * <p>The same secret is used by the {@code JwtDecoder} configured in
 * {@code SecurityConfig}, so tokens are validated by Spring Security before a
 * request reaches a business controller.</p>
 */
@Service
public class JwtTokenService {

    static final String ISSUER = "ilussioness-backend";

    private final JwtEncoder jwtEncoder;
    private final long tokenTtlMinutes;

    public JwtTokenService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtlMinutes = properties.getTokenTtlMinutes();
    }

    public IssuedToken issue(AuthenticatedUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtlMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.email())
                .claim("name", user.name())
                .claim("role", user.role())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, Duration.between(issuedAt, expiresAt).toSeconds());
    }

    /** Token value plus its lifetime in seconds, ready for the JSON response. */
    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
