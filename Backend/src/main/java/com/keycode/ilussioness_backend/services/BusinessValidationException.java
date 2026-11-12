package com.keycode.ilussioness_backend.services;

/**
 * Raised by the business services when a request is syntactically valid but
 * violates an application rule (missing required value, unknown referenced
 * entity, negative amount, ...).
 *
 * <p>The {@link #getCode() code} is a short, non-sensitive machine token that
 * is safe to return to the client. The message is never exposed.</p>
 */
public class BusinessValidationException extends RuntimeException {

    private final String code;

    public BusinessValidationException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Maps a validation code to one of the bounded audit {@code REASON} values
     * accepted by the audit schema, so a rejection can be recorded without
     * leaking the raw validation detail.
     */
    public static String auditReason(String code) {
        if (code == null) {
            return "INVALID_DATA";
        }
        if (code.endsWith("_not_found")) {
            return "NOT_FOUND";
        }
        if (code.contains("required")) {
            return "REQUIRED";
        }
        if (code.endsWith("_too_long")) {
            return "FORMAT";
        }
        return "INVALID_DATA";
    }
}
