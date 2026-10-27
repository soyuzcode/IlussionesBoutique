package com.keycode.ilussioness_backend.quotes.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for the preliminary quote PDF generation.
 *
 * <p>Adapts the quote intake to the current application model: exactly one
 * custom dress plus zero or more prefabricated products.</p>
 */
public record PreliminaryQuoteRequest(

        UUID customerId,

        @Size(max = 64)
        String eventType,

        LocalDate deliveryDate,

        @NotNull
        DressQuoteRequest dress,

        List<PrefabricatedProductQuoteRequest> products
) {

    public PreliminaryQuoteRequest {
        if (products == null) {
            products = List.of();
        }
    }
}
