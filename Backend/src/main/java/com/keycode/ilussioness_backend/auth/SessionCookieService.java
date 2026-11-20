package com.keycode.ilussioness_backend.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and reads the long-lived {@code HttpOnly} session cookie.
 *
 * <p>The cookie holds a JWT signed with a purpose-derived key, so it is never
 * accepted as a bearer token by the resource server. It is scoped to
 * {@code /api/auth} and only exchanged for a short-lived access token through
 * {@code POST /api/auth/session}.</p>
 */
@Service
public class SessionCookieService {

    static final String COOKIE_PATH = "/api/auth";
    private static final String PURPOSE = "session-cookie-v1";

    private final AuthProperties.Session settings;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public SessionCookieService(AuthProperties properties) {
        this.settings = properties.getSession();
        SecretKey key = DerivedKeys.hmacKey(properties.getJwtSecret(), PURPOSE);
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public ResponseCookie issue(String email, String credentialVersion) {
        Duration ttl = Duration.ofDays(settings.getTtlDays());
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtTokenService.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(email)
                .claim("purpose", PURPOSE)
                .claim("pwd", fingerprint(credentialVersion))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return cookie(value, ttl);
    }

    public ResponseCookie clear() {
        return cookie("", Duration.ZERO);
    }

    /**
     * True when the cookie was issued for this account and its password has not
     * changed since, so a password reset signs out every remembered browser.
     */
    public boolean belongsTo(SessionClaims claims, String credentialVersion) {
        return MessageDigest.isEqual(
                fingerprint(credentialVersion).getBytes(StandardCharsets.UTF_8),
                String.valueOf(claims.passwordFingerprint()).getBytes(StandardCharsets.UTF_8));
    }

    /** Claims of a valid, unexpired session cookie, if the request carries one. */
    public Optional<SessionClaims> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> settings.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank() && value.length() <= 4096)
                .findFirst()
                .flatMap(this::decode);
    }

    private Optional<SessionClaims> decode(String value) {
        try {
            Jwt jwt = decoder.decode(value);
            if (!PURPOSE.equals(jwt.getClaimAsString("purpose"))) {
                return Optional.empty();
            }
            if (jwt.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(new SessionClaims(jwt.getSubject(), jwt.getClaimAsString("pwd")));
        } catch (JwtException ex) {
            return Optional.empty();
        }
    }

    private static String fingerprint(String credentialVersion) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(credentialVersion).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record SessionClaims(String email, String passwordFingerprint) {
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(settings.getCookieName(), value)
                .httpOnly(true)
                .secure(settings.isSecure())
                .sameSite(settings.getSameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
