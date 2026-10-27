package com.keycode.ilussioness_backend.quotes.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Custom dress selection of a preliminary quote.
 */
public record DressQuoteRequest(

        @NotNull
        UUID productId,

        @Size(max = 120)
        String measurements,

        @Size(max = 64)
        String color,

        @Size(max = 64)
        String cutType,

        @Size(max = 512)
        String customizations
) {
}
