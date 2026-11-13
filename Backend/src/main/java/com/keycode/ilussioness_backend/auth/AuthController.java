package com.keycode.ilussioness_backend.auth;

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
@CrossOrigin(origins = "*")
public class AuthController {

    private final DemoAccountService demoAccountService;
    private final JwtTokenService jwtTokenService;

    public AuthController(DemoAccountService demoAccountService, JwtTokenService jwtTokenService) {
        this.demoAccountService = demoAccountService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<AuthenticatedUser> user = demoAccountService.authenticate(
                request.email(), request.password());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("error", "invalid_credentials"));
        }

        JwtTokenService.IssuedToken token = jwtTokenService.issue(user.get());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(LoginResponse.from(token, user.get()));
    }
}
