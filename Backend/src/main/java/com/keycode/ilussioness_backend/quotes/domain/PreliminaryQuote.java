package com.keycode.ilussioness_backend.quotes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rendering model for a preliminary quote PDF.
 *
 * <p>The preliminary quote is never persisted: the reference is generated for
 * the document only.</p>
 */
public record PreliminaryQuote(
        String reference,
        String eventType,
        LocalDate deliveryDate,
        String customerName,
        String dressProductId,
        String dressDescription,
        BigDecimal dressPrice,
        List<QuoteLineItem> items,
        BigDecimal subtotal,
        LocalDateTime issueDate
) {
}
