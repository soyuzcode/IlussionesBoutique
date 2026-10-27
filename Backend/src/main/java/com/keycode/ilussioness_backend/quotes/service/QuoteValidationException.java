package com.keycode.ilussioness_backend.quotes.service;

/**
 * Raised when a quote cannot be generated because of invalid input.
 */
public class QuoteValidationException extends RuntimeException {

    private final String reason;

    public QuoteValidationException(String reason) {
        super("Invalid quote: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
