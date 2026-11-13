package com.keycode.ilussioness_backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

/**
 * Backend authority for the single demonstration account.
 *
 * <p>The configured plaintext password is hashed once at startup with BCrypt.
 * Verification always runs the encoder, even when the email does not match, so
 * a caller cannot distinguish "unknown email" from "wrong password" by timing or
 * by the response body.</p>
 */
@Service
public class DemoAccountService {

    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String name;
    private final String role;
    private final String passwordHash;

    public DemoAccountService(AuthProperties properties, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;

        AuthProperties.Demo demo = properties.getDemo();
        this.email = normalizeEmail(require(demo.getEmail(), "app.auth.demo.email"));
        this.name = require(demo.getName(), "app.auth.demo.name").trim();
        this.role = require(demo.getRole(), "app.auth.demo.role").trim();
        this.passwordHash = passwordEncoder.encode(
                require(demo.getPassword(), "app.auth.demo.password"));
    }

    /**
     * @return the authenticated identity, or empty when the email/password pair
     *         is not valid. The same empty result is returned for an unknown
     *         email and for a wrong password.
     */
    public Optional<AuthenticatedUser> authenticate(String candidateEmail, String candidatePassword) {
        if (candidateEmail == null || candidatePassword == null || candidatePassword.isEmpty()) {
            return Optional.empty();
        }

        boolean emailMatches = MessageDigest.isEqual(
                email.getBytes(StandardCharsets.UTF_8),
                normalizeEmail(candidateEmail).getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = passwordEncoder.matches(candidatePassword, passwordHash);

        return emailMatches && passwordMatches
                ? Optional.of(new AuthenticatedUser(email, name, role))
                : Optional.empty();
    }

    private static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String require(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must be configured");
        }
        return value;
    }
}
