package com.keycode.ilussioness_backend.auth;

/**
 * Successful login payload. It intentionally contains only the token and the
 * public user identity: no password, hash, or internal identifier is returned.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public record UserResponse(String name, String email, String role) {
    }

    public static LoginResponse from(JwtTokenService.IssuedToken token, AuthenticatedUser user) {
        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                new UserResponse(user.name(), user.email(), user.role()));
    }
}
