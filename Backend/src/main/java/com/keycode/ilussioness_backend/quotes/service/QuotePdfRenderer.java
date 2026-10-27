package com.keycode.ilussioness_backend.quotes.service;

import com.keycode.ilussioness_backend.quotes.domain.OrderQuote;
import com.keycode.ilussioness_backend.quotes.domain.PreliminaryQuote;
import com.keycode.ilussioness_backend.quotes.domain.QuoteLineItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders Spanish-language PDF quotes with Apache PDFBox 3.x.
 *
 * <p>The document uses the standard Helvetica Type-1 base fonts which support
 * WinAnsi encoding, so Spanish accented characters (á, é, í, ó, ú, ñ) are
 * rendered correctly without embedding additional font files.</p>
 *
 * <p>Coordinate system: PDFBox uses a bottom-left origin. All {@code top}
 * arguments passed to helper methods are expressed as distance from the top of
 * the page, and are converted to PDFBox coordinates by {@code PAGE_HEIGHT - top}
 * inside each helper.</p>
 */
@Component
public class QuotePdfRenderer {

    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 841.89
    private static final float MARGIN = 50f;
    private static final float CONTENT_RIGHT = PDRectangle.A4.getWidth() - MARGIN; // ~545
    private static final float FONT_SIZE_BOUTIQUE = 13f;
    private static final float FONT_SIZE_TITLE = 22f;
    private static final float FONT_SIZE_SECTION = 11f;
    private static final float FONT_SIZE_BODY = 10f;
    private static final float FONT_SIZE_TOTAL = 13f;
    private static final float RULE_WIDTH = 0.5f;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public byte[] renderPreliminaryQuote(PreliminaryQuote quote) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont regular = new PDType1Font(document, Standard14Fonts.FontName.HELVETICA);
            PDFont bold    = new PDType1Font(document, Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawPreliminaryQuote(cs, bold, regular, quote);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate the preliminary quote PDF", ex);
        }
    }

    public byte[] renderOrderQuote(OrderQuote orderQuote) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont regular = new PDType1Font(document, Standard14Fonts.FontName.HELVETICA);
            PDFont bold    = new PDType1Font(document, Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawOrderQuote(cs, bold, regular, orderQuote);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate the order quote PDF", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Preliminary quote rendering
    // -------------------------------------------------------------------------

    private void drawPreliminaryQuote(
            PDPageContentStream cs,
            PDFont bold,
            PDFont regular,
            PreliminaryQuote quote
    ) throws IOException {

        float y = PAGE_HEIGHT - MARGIN; // start near top

        // Header
        text(cs, bold,    FONT_SIZE_TITLE,    "COTIZACION", MARGIN, y); y -= 22f;
        text(cs, regular, FONT_SIZE_BOUTIQUE, "Ilusiones Boutique", MARGIN, y); y -= 18f;
        hline(cs, MARGIN, y, CONTENT_RIGHT);                             y -= 14f;

        text(cs, regular, FONT_SIZE_BODY,
                "Referencia: " + safeText(quote.reference(), "COT-00000000"), MARGIN, y); y -= 14f;
        text(cs, regular, FONT_SIZE_BODY,
                "Fecha de emision: " + formatDateTime(quote.issueDate()), MARGIN, y); y -= 22f;

        // Customer section
        if (quote.customerName() != null) {
            sectionHeader(cs, bold, "DATOS DEL CLIENTE", MARGIN, y); y -= 20f;
            kv(cs, regular, "Cliente", quote.customerName(), MARGIN, y); y -= 14f;
            y -= 8f;
        }

        // Event section
        sectionHeader(cs, bold, "DETALLES DEL EVENTO", MARGIN, y); y -= 20f;
        kv(cs, regular, "Evento",           safeText(quote.eventType(), "-"), MARGIN, y); y -= 14f;
        kv(cs, regular, "Fecha de entrega", formatDate(quote.deliveryDate()),  MARGIN, y); y -= 22f;

        // Dress section
        sectionHeader(cs, bold, "VESTIDO PERSONALIZADO", MARGIN, y); y -= 20f;
        List<String> dressLines = wrap(quote.dressDescription(), 72);
        for (String line : dressLines) {
            text(cs, regular, FONT_SIZE_BODY, line, MARGIN, y); y -= 13f;
        }
        kv(cs, bold, "Precio", formatAmount(quote.dressPrice()), MARGIN, y); y -= 22f;

        // Additional products
        sectionHeader(cs, bold, "PRODUCTOS ADICIONALES", MARGIN, y); y -= 20f;
        List<QuoteLineItem> extraItems = extraItems(quote);
        if (extraItems.isEmpty()) {
            text(cs, regular, FONT_SIZE_BODY, "Sin productos adicionales.", MARGIN, y); y -= 14f;
        } else {
            for (QuoteLineItem item : extraItems) {
                List<String> descLines = wrap(safeText(item.description(), "Producto"), 70);
                for (String line : descLines) {
                    text(cs, regular, FONT_SIZE_BODY, line, MARGIN, y); y -= 13f;
                }
                text(cs, regular, FONT_SIZE_BODY,
                        "  Cant: " + item.quantity()
                                + "  P.Unit: " + formatAmount(item.unitPrice())
                                + "  Subtotal: " + formatAmount(item.lineTotal()),
                        MARGIN, y);
                y -= 16f;
            }
        }

        y -= 6f;
        hline(cs, MARGIN, y, CONTENT_RIGHT); y -= 16f;
        text(cs, bold, FONT_SIZE_TOTAL,
                "TOTAL COTIZADO: " + formatAmount(quote.subtotal()), MARGIN, y);

        // Footer
        text(cs, regular, 8f,
                "Esta cotizacion no constituye un pedido confirmado.",
                MARGIN, MARGIN + 10f);
    }

    // -------------------------------------------------------------------------
    // Order quote rendering
    // -------------------------------------------------------------------------

    private void drawOrderQuote(
            PDPageContentStream cs,
            PDFont bold,
            PDFont regular,
            OrderQuote orderQuote
    ) throws IOException {

        float y = PAGE_HEIGHT - MARGIN;

        // Header
        text(cs, bold,    FONT_SIZE_TITLE,    "COTIZACION DE PEDIDO", MARGIN, y); y -= 22f;
        text(cs, regular, FONT_SIZE_BOUTIQUE, "Ilusiones Boutique",   MARGIN, y); y -= 18f;
        hline(cs, MARGIN, y, CONTENT_RIGHT);                                       y -= 14f;

        text(cs, regular, FONT_SIZE_BODY,
                "Pedido: " + safeText(orderQuote.orderId(), "-"), MARGIN, y); y -= 14f;
        if (orderQuote.createdAt() != null) {
            text(cs, regular, FONT_SIZE_BODY,
                    "Fecha: " + orderQuote.createdAt(), MARGIN, y); y -= 14f;
        }
        y -= 8f;

        // Customer
        if (orderQuote.customerName() != null) {
            sectionHeader(cs, bold, "CLIENTE", MARGIN, y); y -= 20f;
            kv(cs, regular, "Nombre", orderQuote.customerName(), MARGIN, y); y -= 14f;
            y -= 8f;
        }

        // Event
        sectionHeader(cs, bold, "EVENTO", MARGIN, y); y -= 20f;
        kv(cs, regular, "Tipo",    safeText(orderQuote.eventType(), "-"),       MARGIN, y); y -= 14f;
        kv(cs, regular, "Entrega", safeText(orderQuote.deliveryDate(), "-"),    MARGIN, y); y -= 14f;
        kv(cs, regular, "Estado",  safeText(orderQuote.orderStatus(), "-"),     MARGIN, y); y -= 22f;

        // Products
        sectionHeader(cs, bold, "PRODUCTOS", MARGIN, y); y -= 20f;
        if (orderQuote.items() == null || orderQuote.items().isEmpty()) {
            text(cs, regular, FONT_SIZE_BODY, "Sin productos registrados.", MARGIN, y); y -= 14f;
        } else {
            for (QuoteLineItem item : orderQuote.items()) {
                List<String> descLines = wrap(safeText(item.description(), "Producto"), 70);
                for (String line : descLines) {
                    text(cs, regular, FONT_SIZE_BODY, line, MARGIN, y); y -= 13f;
                }
                text(cs, regular, FONT_SIZE_BODY,
                        "  Cant: " + item.quantity()
                                + "  P.Unit: " + formatAmount(item.unitPrice())
                                + "  Subtotal: " + formatAmount(item.lineTotal()),
                        MARGIN, y);
                y -= 16f;
            }
        }

        y -= 6f;
        hline(cs, MARGIN, y, CONTENT_RIGHT); y -= 16f;
        text(cs, bold, FONT_SIZE_TOTAL,
                "TOTAL DEL PEDIDO: " + formatAmount(orderQuote.total()), MARGIN, y); y -= 18f;

        // Payment summary
        if (orderQuote.totalPaid() != null) {
            text(cs, regular, FONT_SIZE_BODY,
                    "Pagado: " + formatAmount(orderQuote.totalPaid()), MARGIN, y); y -= 14f;
            text(cs, regular, FONT_SIZE_BODY,
                    "Saldo pendiente: " + formatAmount(orderQuote.balance()), MARGIN, y); y -= 14f;
            kv(cs, regular, "Estado de pago", safeText(orderQuote.paymentStatus(), "-"), MARGIN, y);
        }

        text(cs, regular, 8f,
                "Los precios reflejan los montos acordados al momento del pedido.",
                MARGIN, MARGIN + 10f);
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    /**
     * Draws text at {@code (left, y)} where {@code y} is in PDFBox coordinates
     * (measured from bottom-left of the page).
     */
    private void text(
            PDPageContentStream cs,
            PDFont font,
            float size,
            String value,
            float left,
            float y
    ) throws IOException {
        // Sanitize: PDFBox WinAnsi cannot encode characters outside Latin-1.
        String safe = latinSafe(safeText(value, ""));
        if (safe.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(left, y);
        cs.showText(safe);
        cs.endText();
    }

    /** Draws a horizontal rule. {@code y} is in PDFBox coordinates. */
    private void hline(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.setLineWidth(RULE_WIDTH);
        cs.setStrokingColor(0.4f, 0.4f, 0.4f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    /** Section header (bold label + underline). */
    private void sectionHeader(PDPageContentStream cs, PDFont bold, String title,
                                float left, float y) throws IOException {
        text(cs, bold, FONT_SIZE_SECTION, title, left, y);
        hline(cs, left, y - 3f, CONTENT_RIGHT);
    }

    /** Key-value pair on one line. */
    private void kv(PDPageContentStream cs, PDFont font, String key, String value,
                    float left, float y) throws IOException {
        text(cs, font, FONT_SIZE_BODY, key + ": " + safeText(value, "-"), left, y);
    }

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    /** Returns only the extra (non-dress) items from a preliminary quote's item list. */
    private List<QuoteLineItem> extraItems(PreliminaryQuote quote) {
        if (quote.items() == null || quote.items().isEmpty()) return List.of();
        // The first item is always the dress; additional items follow.
        if (quote.items().size() <= 1) return List.of();
        return quote.items().subList(1, quote.items().size());
    }

    private List<String> wrap(String value, int maxChars) {
        String text = safeText(value, "");
        if (text.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;
            StringBuilder current = new StringBuilder();
            for (String word : trimmed.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (candidate.length() > maxChars && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
        }
        return lines;
    }

    private String safeText(String value, String fallback) {
        if (value == null) return fallback;
        String t = value.trim();
        return t.isEmpty() ? fallback : t;
    }

    private String formatAmount(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return "$ " + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    /**
     * Replaces characters outside the WinAnsi (ISO-8859-1 superset) range with
     * a Latin approximation so that PDFBox never throws an encoding exception.
     * Common Spanish characters (á é í ó ú ü ñ Á É Í Ó Ú Ü Ñ ¿ ¡) are all
     * within WinAnsi and are preserved.
     */
    private String latinSafe(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c < 256) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
