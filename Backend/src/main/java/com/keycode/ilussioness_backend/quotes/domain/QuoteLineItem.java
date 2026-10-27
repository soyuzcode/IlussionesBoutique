package com.keycode.ilussioness_backend.quotes.domain;

import java.math.BigDecimal;

/**
 * A single line of a quote document.
 *
 * @param productId   stable identifier of the product (dress or prefabricated item)
 * @param description human readable description used for rendering
 * @param quantity    units quoted
 * @param unitPrice   agreed unit price (never taken from client input)
 * @param lineTotal   quantity * unit price, already rounded to currency scale
 */
public record QuoteLineItem(
        String productId,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static QuoteLineItem of(String productId, String description, int quantity, BigDecimal unitPrice) {
        BigDecimal safeUnitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal safeLineTotal = safeUnitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        return new QuoteLineItem(productId, description, quantity, safeUnitPrice, safeLineTotal);
    }
}
