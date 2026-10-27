package com.keycode.ilussioness_backend.quotes.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Prefabricated product line of a preliminary quote.
 */
public record PrefabricatedProductQuoteRequest(

        @NotNull
        UUID productId,

        @Min(1)
        int quantity
) {
}
