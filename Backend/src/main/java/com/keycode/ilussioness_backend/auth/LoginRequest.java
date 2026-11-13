package com.keycode.ilussioness_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credentials accepted by {@code POST /api/auth/login}. */
public record LoginRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(max = 200, message = "password must be at most 200 characters")
        String password
) {
}
