package com.keycode.ilussioness_backend.auth;

/** Public identity of an authenticated user (never includes credentials). */
public record AuthenticatedUser(String email, String name, String role) {
}
