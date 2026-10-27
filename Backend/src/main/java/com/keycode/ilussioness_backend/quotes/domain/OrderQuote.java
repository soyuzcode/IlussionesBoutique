package com.keycode.ilussioness_backend.quotes.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rendering model for the PDF of an existing order.
 *
 * <p>Only data that is actually available through the current entity model is
 * mapped. Fields that are not yet persisted remain {@code null} and the renderer
 * will display a dash placeholder.</p>
 */
public record OrderQuote(
        String orderId,
        String eventType,
        /** Formatted delivery date string (dd/MM/yyyy) or null. */
        String deliveryDate,
        /** Formatted creation date string (dd/MM/yyyy) or null. */
        String createdAt,
        String customerName,
        String orderStatus,
        String paymentStatus,
        List<QuoteLineItem> items,
        BigDecimal total,
        /** Sum of recorded payments; null if payment data is unavailable. */
        BigDecimal totalPaid,
        /** Computed outstanding balance (total minus totalPaid); null if totalPaid is null. */
        BigDecimal balance
) {
}
