package com.keycode.ilussioness_backend.quotes.api;

import com.keycode.ilussioness_backend.quotes.api.dto.PreliminaryQuoteRequest;
import com.keycode.ilussioness_backend.quotes.domain.OrderQuote;
import com.keycode.ilussioness_backend.quotes.domain.PreliminaryQuote;
import com.keycode.ilussioness_backend.quotes.service.QuoteNotFoundException;
import com.keycode.ilussioness_backend.quotes.service.QuotePdfRenderer;
import com.keycode.ilussioness_backend.quotes.service.QuoteService;
import com.keycode.ilussioness_backend.quotes.service.QuoteValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for quote-related endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/quotes/pdf} — generate a preliminary quote PDF without
 *       creating any business record.</li>
 *   <li>{@code GET /api/orders/{id}/quote.pdf} — generate a PDF from an
 *       existing, persisted order.</li>
 * </ul>
 */
@RestController
@CrossOrigin(origins = "*")
public class QuoteController {

    private final QuoteService quoteService;
    private final QuotePdfRenderer renderer;

    public QuoteController(QuoteService quoteService, QuotePdfRenderer renderer) {
        this.quoteService = quoteService;
        this.renderer = renderer;
    }

    // -------------------------------------------------------------------------
    // Feature A — preliminary quote
    // -------------------------------------------------------------------------

    /**
     * Generates a downloadable PDF from a preliminary quote request.
     *
     * <p>This endpoint is side-effect free: no order, payment, appointment, or
     * inventory record is created.</p>
     *
     * @param request validated quote intake
     * @return PDF bytes with Content-Type application/pdf
     */
    @PostMapping("/api/quotes/pdf")
    public ResponseEntity<?> generatePreliminaryQuotePdf(
            @Valid @RequestBody PreliminaryQuoteRequest request
    ) {
        PreliminaryQuote quote = quoteService.createPreliminaryQuote(request);
        byte[] pdfBytes = renderer.renderPreliminaryQuote(quote);

        String filename = "cotizacion-" + quote.reference() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdfBytes);
    }

    // -------------------------------------------------------------------------
    // Feature B — order quote PDF
    // -------------------------------------------------------------------------

    /**
     * Generates a PDF for an existing persisted order.
     *
     * <p>This endpoint is read-only: it does not modify the order or any related
     * entity. Prices are taken from the persisted order data.</p>
     *
     * @param id the order identifier
     * @return PDF bytes with Content-Type application/pdf, or 404 if not found
     */
    @GetMapping("/api/orders/{id}/quote.pdf")
    public ResponseEntity<?> generateOrderQuotePdf(@PathVariable("id") String id) {
        OrderQuote orderQuote = quoteService.createOrderQuote(id);
        byte[] pdfBytes = renderer.renderOrderQuote(orderQuote);

        String filename = "pedido-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdfBytes);
    }

    // -------------------------------------------------------------------------
    // Exception handlers
    // -------------------------------------------------------------------------

    @ExceptionHandler(QuoteValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            QuoteValidationException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "quote_validation_failed",
                "reason", ex.getReason()
        ));
    }

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(
            QuoteNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "order_not_found",
                "message", ex.getMessage()
        ));
    }
}
