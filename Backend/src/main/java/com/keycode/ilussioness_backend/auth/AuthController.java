package com.keycode.ilussioness_backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Demo authentication endpoint.
 *
 * <p>The backend is the only authority that decides whether a login is valid.
 * Invalid email and wrong password produce the same generic {@code 401} body so
 * the endpoint does not disclose whether an account exists.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String SCRIPT_REQUEST_HEADER = "X-Requested-With";
    static final String SCRIPT_REQUEST_VALUE = "XMLHttpRequest";

    private final AccountService accountService;
    private final JwtTokenService jwtTokenService;
    private final PasswordResetService passwordResetService;
    private final SessionCookieService sessionCookieService;

    public AuthController(
            AccountService accountService,
            JwtTokenService jwtTokenService,
            PasswordResetService passwordResetService,
            SessionCookieService sessionCookieService
    ) {
        this.accountService = accountService;
        this.jwtTokenService = jwtTokenService;
        this.passwordResetService = passwordResetService;
        this.sessionCookieService = sessionCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<AuthenticatedUser> user = accountService.authenticate(
                request.email(), request.password());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("error", "invalid_credentials"));
        }

        JwtTokenService.IssuedToken token = jwtTokenService.issue(user.get());
        ResponseCookie cookie = request.keepSession()
                ? accountService.findAccount(user.get().email()).map(this::sessionCookie)
                        .orElseGet(sessionCookieService::clear)
                : sessionCookieService.clear();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(LoginResponse.from(token, user.get()));
    }

    /**
     * Exchanges the {@code HttpOnly} session cookie for a fresh access token and
     * renews the cookie (sliding expiration). Answers {@code 401} and clears the
     * cookie when it is missing, expired or no longer matches the account.
     */
    @PostMapping("/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        if (!isScriptRequest(request)) {
            return csrfRejected();
        }
        Optional<AccountService.Account> account = sessionCookieService.read(request)
                .flatMap(claims -> accountService.findAccount(claims.email())
                        .filter(found -> sessionCookieService.belongsTo(claims, accountService.credentialVersion(found))));

        if (account.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.SET_COOKIE, sessionCookieService.clear().toString())
                    .body(Map.of("error", "no_session"));
        }

        AccountService.Account found = account.get();
        AuthenticatedUser user = new AuthenticatedUser(found.email(), found.name(), found.role());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, sessionCookie(found).toString())
                .body(LoginResponse.from(jwtTokenService.issue(user), user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        if (!isScriptRequest(request)) {
            return csrfRejected();
        }
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.clear().toString())
                .build();
    }

    /**
     * Always answers {@code 202} with the same body, whether or not the e-mail
     * belongs to an account.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", "accepted"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private ResponseCookie sessionCookie(AccountService.Account account) {
        return sessionCookieService.issue(account.email(), accountService.credentialVersion(account));
    }

    /**
     * CSRF guard for the cookie endpoints: a cross-site form cannot add custom
     * headers, and a cross-site script needs a CORS preflight that only the
     * configured frontend origins pass.
     */
    private static boolean isScriptRequest(HttpServletRequest request) {
        return SCRIPT_REQUEST_VALUE.equals(request.getHeader(SCRIPT_REQUEST_HEADER));
    }

    private static ResponseEntity<Map<String, String>> csrfRejected() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("error", "csrf_header_required"));
    }
}
