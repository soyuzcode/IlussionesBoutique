package com.keycode.ilussioness_backend.quotes.service;

/**
 * Raised when a quote targets an order that does not exist.
 */
public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
