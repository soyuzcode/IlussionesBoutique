package com.keycode.ilussioness_backend.quotes.service;

import com.keycode.ilussioness_backend.models.Card;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.models.Cup;
import com.keycode.ilussioness_backend.models.Dress;
import com.keycode.ilussioness_backend.models.Order;
import com.keycode.ilussioness_backend.models.Product;
import com.keycode.ilussioness_backend.quotes.api.dto.DressQuoteRequest;
import com.keycode.ilussioness_backend.quotes.api.dto.PreliminaryQuoteRequest;
import com.keycode.ilussioness_backend.quotes.api.dto.PrefabricatedProductQuoteRequest;
import com.keycode.ilussioness_backend.quotes.domain.OrderQuote;
import com.keycode.ilussioness_backend.quotes.domain.PreliminaryQuote;
import com.keycode.ilussioness_backend.quotes.domain.QuoteLineItem;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import com.keycode.ilussioness_backend.repositories.OrderRepository;
import com.keycode.ilussioness_backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class QuoteService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of("WEDDING", "QUINCEANERA");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public QuoteService(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository
    ) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    // -------------------------------------------------------------------------
    // Feature A — preliminary (non-persisted) quote
    // -------------------------------------------------------------------------

    /**
     * Builds a preliminary quote from the intake data.
     *
     * <p>The quote is <em>never persisted</em> and never touches order/payment
     * state, so generating a preliminary quote is completely side-effect free.</p>
     *
     * @throws QuoteValidationException if required data is missing or invalid
     */
    public PreliminaryQuote createPreliminaryQuote(PreliminaryQuoteRequest request) {
        if (request == null || request.dress() == null || request.dress().productId() == null) {
            throw new QuoteValidationException("DRESS_REQUIRED");
        }

        List<PrefabricatedProductQuoteRequest> prefabItems = new ArrayList<>();
        if (request.products() != null) {
            prefabItems.addAll(request.products());
        }

        // Validate and load the dress product from the database.
        Product dressProduct = productRepository.findById(request.dress().productId())
                .orElseThrow(() -> new QuoteValidationException("DRESS_NOT_FOUND"));
        if (!(dressProduct instanceof Dress)) {
            throw new QuoteValidationException("DRESS_NOT_FOUND");
        }

        List<QuoteLineItem> items = new ArrayList<>();
        BigDecimal dressPrice = money(dressProduct.getPrice());
        // Dress is always the first line item.
        items.add(QuoteLineItem.of(
                request.dress().productId().toString(),
                dressDescription(request.dress(), dressProduct),
                1,
                dressPrice
        ));

        // Validate and load each additional prefabricated product.
        for (PrefabricatedProductQuoteRequest item : prefabItems) {
            if (item == null || item.productId() == null) {
                throw new QuoteValidationException("PRODUCT_ID_INVALID");
            }
            if (item.quantity() < 1) {
                throw new QuoteValidationException("QUANTITY_INVALID");
            }
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new QuoteValidationException("PRODUCT_NOT_FOUND"));
            if (product instanceof Dress) {
                throw new QuoteValidationException("PRODUCT_NOT_PREFABRICATED");
            }
            BigDecimal unitPrice = money(product.getPrice());
            items.add(QuoteLineItem.of(
                    item.productId().toString(),
                    productDescription(product),
                    item.quantity(),
                    unitPrice
            ));
        }

        BigDecimal subtotal = items.stream()
                .map(QuoteLineItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new PreliminaryQuote(
                "COT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                resolveEventType(request.eventType()),
                request.deliveryDate(),
                findCustomerName(request.customerId()),
                request.dress().productId().toString(),
                dressDescription(request.dress(), dressProduct),
                dressPrice,
                items,
                subtotal,
                LocalDateTime.now()
        );
    }

    // -------------------------------------------------------------------------
    // Feature B — PDF for an existing order
    // -------------------------------------------------------------------------

    /**
     * Builds the rendering model of an existing order quote.
     *
     * <p>The order is <em>never modified</em>. Prices are taken from the persisted
     * order data (not current catalog prices).</p>
     *
     * @throws QuoteNotFoundException if the order does not exist
     */
    public OrderQuote createOrderQuote(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new QuoteNotFoundException(orderId));

        List<QuoteLineItem> items = new ArrayList<>();
        if (order.getProducts() != null) {
            for (Product product : order.getProducts()) {
                if (product == null) continue;
                int quantity = 1;
                if (product instanceof Card card) {
                    quantity = card.getQuantity();
                } else if (product instanceof Cup cup) {
                    quantity = cup.getQuantity();
                }
                // Use persisted price from the entity (not current catalog price).
                BigDecimal unitPrice = money(product.getPrice());
                items.add(QuoteLineItem.of(
                        product.getProductId().toString(),
                        productDescription(product),
                        quantity,
                        unitPrice
                ));
            }
        }

        BigDecimal total = items.stream()
                .map(QuoteLineItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Format delivery date if available.
        String deliveryDateStr = null;
        if (order.getDeliveryDate() != null) {
            deliveryDateStr = DATE_FMT.format(
                    order.getDeliveryDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
            );
        }

        return new OrderQuote(
                order.getOrderId(),
                order.getEventType(),
                deliveryDateStr,
                null,           // createdAt: Order entity does not expose a creation timestamp
                null,           // customerName: Order entity does not have a Customer association yet
                null,           // orderStatus: not mapped in current entity
                null,           // paymentStatus: not mapped in current entity
                items,
                total,
                null,           // totalPaid: payment entity not mapped yet
                null            // balance: derived from totalPaid
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String resolveEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        // Accept both "QUINCEAÑERA" and "QUINCEANERA" (without tilde) from clients.
        String normalized = eventType.trim().toUpperCase(Locale.ROOT)
                .replace("\u00D1", "N"); // Ñ → N for comparison
        String raw = eventType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_EVENT_TYPES.contains(normalized) && !SUPPORTED_EVENT_TYPES.contains(raw)) {
            throw new QuoteValidationException("EVENT_TYPE_INVALID");
        }
        // Return the raw value supplied by the client for display purposes.
        return raw;
    }

    private String findCustomerName(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findById(customerId)
                .map(Customer::getName)
                .orElseThrow(() -> new QuoteValidationException("CUSTOMER_NOT_FOUND"));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String dressDescription(DressQuoteRequest dress, Product dressProduct) {
        List<String> parts = new ArrayList<>();
        if (dressProduct.getMaterials() != null && !dressProduct.getMaterials().isBlank()) {
            parts.add(dressProduct.getMaterials());
        }
        if (dress.measurements() != null && !dress.measurements().isBlank()) {
            parts.add("Medidas: " + dress.measurements());
        }
        if (dress.color() != null && !dress.color().isBlank()) {
            parts.add("Color: " + dress.color());
        }
        if (dress.cutType() != null && !dress.cutType().isBlank()) {
            parts.add("Corte: " + dress.cutType());
        }
        if (dress.customizations() != null && !dress.customizations().isBlank()) {
            parts.add("Notas: " + dress.customizations());
        }
        return parts.isEmpty() ? "Vestido personalizado" : String.join(", ", parts);
    }

    private String productDescription(Product product) {
        if (product.getMaterials() != null && !product.getMaterials().isBlank()) {
            return product.getMaterials();
        }
        return "Producto " + product.getProductId();
    }
}
