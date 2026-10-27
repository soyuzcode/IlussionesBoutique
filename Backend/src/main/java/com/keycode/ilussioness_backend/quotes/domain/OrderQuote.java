package com.keycode.ilussioness_backend.quotes.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Rendering model for the PDF of an existing order.
 *
 * <p>Only data that is actually persisted today is mapped; missing columns
 * are simply absent from this model.</p>
 */
public record OrderQuote(
        String orderId,
        String eventType,
        Date deliveryDate,
        String customerName,
        List<QuoteLineItem> items,
        BigDecimal total
) {
}
