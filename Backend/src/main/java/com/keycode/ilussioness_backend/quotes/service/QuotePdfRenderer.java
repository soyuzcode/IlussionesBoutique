package com.keycode.ilussioness_backend.quotes.service;

import com.keycode.ilussioness_backend.quotes.domain.OrderQuote;
import com.keycode.ilussioness_backend.quotes.domain.PreliminaryQuote;
import com.keycode.ilussioness_backend.quotes.domain.QuoteLineItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.BaseEncoding;
import org.apache.pdfbox.pdmodel.font.PDFFont;
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
 * Renders Spanish-language PDF quotes with Apache PDFBox.
 *
 * <p>The document uses the standard Helvetica base fonts with the WinAnsi
 * encoding so Spanish accented characters (á, é, í, ó, ú, ñ) are rendered
 * correctly without embedding additional font files.</p>
 */
@Component
public class QuotePdfRenderer {

    private static final float PAGE_HEIGHT = 842f;
    private static final float MARGIN = 50f;
    private static final float CONTENT_RIGHT = 545f;
    private static final float FONT_SIZE_BOUTIQUE = 18f;
    private static final float FONT_SIZE_TITLE = 26f;
    private static final float FONT_SIZE_SECTION = 12f;
    private static final float FONT_SIZE_BODY = 11f;
    private static final float FONT_SIZE_BOLD = 12f;
    private static final float FONT_SIZE_TOTAL = 16f;
    private static final float RULE_WIDTH = 1f;
    private static final int COLOR_TEXT = 0xFF000000;
    private static final int COLOR_RULE = 0xFF666666;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    public byte[] renderPreliminaryQuote(PreliminaryQuote quote) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4, true, true);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDFFont regularFont = PDFFont.loadFont(document, Standard14Fonts.FontName.HELVETICA, BaseEncoding.WinAnsi);
                PDFFont boldFont = PDFFont.loadFont(document, Standard14Fonts.FontName.HELVETICA_BOLD, BaseEncoding.WinAnsi);
                drawPreliminaryQuote(content, boldFont, regularFont, quote);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate the quote PDF", ex);
        }
    }

    public byte[] renderOrderQuote(OrderQuote orderQuote) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4, true, true);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDFFont regularFont = PDFFont.loadFont(document, Standard14Fonts.FontName.HELVETICA, BaseEncoding.WinAnsi);
                PDFFont boldFont = PDFFont.loadFont(document, Standard14Fonts.FontName.HELVETICA_BOLD, BaseEncoding.WinAnsi);
                drawOrderQuote(content, boldFont, regularFont, orderQuote);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate the quote PDF", ex);
        }
    }

    private void drawPreliminaryQuote(
            PDPageContentStream content,
            PDFFont boldFont,
            PDFFont regularFont,
            PreliminaryQuote quote
    ) throws IOException {
        text(content, boldFont, FONT_SIZE_TITLE, "COTIZACIÓN", MARGIN, 848);
        text(content, regularFont, FONT_SIZE_BOUTIQUE, "Ilusiones Boutique", MARGIN, 810);
        line(content, MARGIN, 826, CONTENT_RIGHT, 826);

        text(content, regularFont, FONT_SIZE_BODY, "Referencia: " + safeText(quote.reference(), "COT-00000000"), MARGIN, 800);
        text(content, regularFont, FONT_SIZE_BODY, "Fecha: " + formatDateTime(quote.issueDate()), MARGIN, 778);

        drawSection(content, boldFont, "DATOS DEL CLIENTE", 726);
        if (quote.customerName() != null) {
            keyValue(content, regularFont, "Cliente", quote.customerName(), MARGIN, 688);
            keyValue(content, regularFont, "Identificador", quote.customerId(), MARGIN, 670);
        }

        drawSection(content, boldFont, "DETALLES DEL EVENTO", 596);
        keyValue(content, regularFont, "Evento", quote.eventType(), MARGIN, 556);
        keyValue(content, regularFont, "Fecha de entrega", formatDate(quote.deliveryDate()), MARGIN, 538);

        drawSection(content, boldFont, "VESTIDO PERSONALIZADO", 484);
        keyValue(content, regularFont, "Vestido", quote.dressDescription(), MARGIN, 446);
        keyValue(content, boldFont, "Precio", formatAmount(quote.dressPrice()), MARGIN, 428);

        drawSection(content, boldFont, "PRODUCTOS ADICIONALES", 366);
        float cursor = 328;
        if (quote.items() == null || quote.items().isEmpty()) {
            text(content, regularFont, FONT_SIZE_BODY, "Sin productos adicionales.", MARGIN, cursor);
            cursor -= 18;
        } else {
            for (QuoteLineItem item : quote.items()) {
                List<String> lines = wrap(safeText(item.description(), "Producto"), 46);
                float lineTop = cursor;
                for (String line : lines) {
                    text(content, regularFont, FONT_SIZE_BODY, line, lineTop, MARGIN);
                    lineTop -= 16;
                }
                text(
                        content,
                        regularFont,
                        FONT_SIZE_BODY,
                        "Cantidad: " + item.quantity()
                                + "   Precio unitario: " + formatAmount(item.unitPrice())
                                + "   Total: " + formatAmount(item.lineTotal()),
                        lineTop,
                        MARGIN
                );
                cursor = lineTop - 12;
            }
        }

        text(content, boldFont, 14, "Subtotal: " + formatAmount(quote.subtotal()), MARGIN, cursor - 20);
        text(content, boldFont, FONT_SIZE_TOTAL, "Total cotizado: " + formatAmount(quote.subtotal()), MARGIN, cursor - 46);

        text(content, regularFont, 10, "Esta cotizacion no constituye un pedido confirmado.", MARGIN, 60);
    }

    private void drawOrderQuote(
            PDPageContentStream content,
            PDFFont boldFont,
            PDFFont regularFont,
            OrderQuote orderQuote
    ) throws IOException {
        text(content, boldFont, FONT_SIZE_TITLE, "COTIZACIÓN", MARGIN, 848);
        text(content, regularFont, FONT_SIZE_BOUTIQUE, "Ilusiones Boutique", MARGIN, 810);
        line(content, MARGIN, 826, CONTENT_RIGHT, 826);

        text(content, regularFont, FONT_SIZE_BODY, "Referencia: " + safeText(orderQuote.orderId(), "ORDER"), MARGIN, 800);
        text(content, regularFont, FONT_SIZE_BODY, "Fecha: " + formatOrderDate(orderQuote.deliveryDate()), MARGIN, 778);

        drawSection(content, boldFont, "DETALLES DEL EVENTO", 596);
        keyValue(content, regularFont, "Evento", orderQuote.eventType(), MARGIN, 556);
        keyValue(content, regularFont, "Fecha de entrega", formatOrderDate(orderQuote.deliveryDate()), MARGIN, 538);

        drawSection(content, boldFont, "PRODUCTOS", 452);
        float cursor = 420;
        if (orderQuote.items() == null || orderQuote.items().isEmpty()) {
            text(content, regularFont, FONT_SIZE_BODY, "Sin productos registrados.", MARGIN, cursor);
            cursor -= 18;
        } else {
            for (QuoteLineItem item : orderQuote.items()) {
                List<String> lines = wrap(safeText(item.description(), "Producto"), 46);
                float lineTop = cursor;
                for (String line : lines) {
                    text(content, regularFont, FONT_SIZE_BODY, line, lineTop, MARGIN);
                    lineTop -= 16;
                }
                text(
                        content,
                        regularFont,
                        FONT_SIZE_BODY,
                        "Cantidad: " + item.quantity()
                                + "   Precio unitario: " + formatAmount(item.unitPrice())
                                + "   Total: " + formatAmount(item.lineTotal),
                        lineTop,
                        MARGIN
                );
                cursor = lineTop - 12;
            }
        }

        text(content, boldFont, 14, "Total del pedido: " + formatAmount(orderQuote.total()), MARGIN, cursor - 20);
        text(content, boldFont, FONT_SIZE_TOTAL, "Total cotizado: " + formatAmount(orderQuote.total()), MARGIN, cursor - 46);

        text(content, regularFont, 10, "Esta cotizacion no constituye un pedido confirmado.", MARGIN, 60);
    }

    private void drawSection(PDPageContentStream content, PDFFont boldFont, String title, float top) throws IOException {
        text(content, boldFont, FONT_SIZE_SECTION, title, MARGIN, top);
        line(content, MARGIN, top - 16, CONTENT_RIGHT, top - 16);
    }

    private void keyValue(PDPageContentStream content, PDFFont font, String key, String value, float left, float top) throws IOException {
        text(content, font, FONT_SIZE_BODY, key + ": " + safeText(value, "-"), left, top);
    }

    private void text(
            PDPageContentStream content,
            PDFFont font,
            float size,
            String value,
            float left,
            float top
    ) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(COLOR_TEXT);
        content.newLineAtOffset(left, PAGE_HEIGHT - top);
        content.showText(value);
        content.endText();
    }

    private void line(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.setLineWidth(RULE_WIDTH);
        content.setNonStrokingColor(COLOR_RULE);
        content.beginPath();
        content.moveTo(x1, PAGE_HEIGHT - y1);
        content.lineTo(x2, PAGE_HEIGHT - y2);
        content.stroke();
    }

    private List<String> wrap(String value, int maxLineLength) {
        String text = safeText(value, "");
        if (text.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : trimmed.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (candidate.length() > maxLineLength && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String formatAmount(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return DATE_FORMATTER.format(dateTime);
    }

    private String formatOrderDate(java.util.Date date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(new java.util.Date(date.getTime()));
    }
}
