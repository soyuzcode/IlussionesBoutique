package com.keycode.ilussioness_backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the development/demo authentication.
 *
 * <p>The demo account is intentionally configured on the backend only. The
 * values can be overridden with environment variables, so no real credential
 * needs to be committed to the repository.</p>
 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** HMAC secret used to sign and verify the demo JWTs. Minimum 32 bytes. */
    private String jwtSecret;

    /** Lifetime of an issued demo token, in minutes. */
    private long tokenTtlMinutes = 60;

    private final Demo demo = new Demo();

    private final Session session = new Session();

    /**
     * Browser origins allowed to call {@code /api/auth/**} with credentials
     * (the session cookie). Origin patterns such as
     * {@code https://app-*-team.vercel.app} are accepted.
     */
    private java.util.List<String> allowedOrigins = new java.util.ArrayList<>();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(long tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public Demo getDemo() {
        return demo;
    }

    public Session getSession() {
        return session;
    }

    public java.util.List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(java.util.List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /** Long-lived, HttpOnly session cookie used to restore a login. */
    public static class Session {

        private String cookieName = "ilussiones_session";
        private long ttlDays = 14;
        private boolean secure = true;
        /** {@code None} is required while the frontend and API live on different sites. */
        private String sameSite = "None";

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public long getTtlDays() {
            return ttlDays;
        }

        public void setTtlDays(long ttlDays) {
            this.ttlDays = ttlDays;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }

    /**
     * Single demonstration account. The password is hashed with BCrypt when the
     * service starts; it is never written to the database or returned by the API.
     */
    public static class Demo {

        private String email;
        private String password;
        private String name = "Admin";
        private String role = "ADMIN";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
