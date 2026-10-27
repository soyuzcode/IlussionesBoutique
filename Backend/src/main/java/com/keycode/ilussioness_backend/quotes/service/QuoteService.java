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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class QuoteService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of("WEDDING", "QUINCEAÑERA");

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

    /**
     * Builds a preliminary quote from the intake data.
     *
     * <p>The quote is never persisted and never touches order/payment state, so
     * generating a preliminary quote is side-effect free.</p>
     */
    public PreliminaryQuote createPreliminaryQuote(PreliminaryQuoteRequest request) {
        if (request == null || request.dress() == null || request.dress().productId() == null) {
            throw new QuoteValidationException("DRESS_REQUIRED");
        }

        List<PrefabricatedProductQuoteRequest> prefabricatedItems = new ArrayList<>();
        if (request.products() != null) {
            prefabricatedItems.addAll(request.products());
        }

        Product dress = productRepository.findById(request.dress().productId())
                .orElseThrow(() -> new QuoteValidationException("DRESS_NOT_FOUND"));
        if (!(dress instanceof Dress)) {
            throw new QuoteValidationException("DRESS_NOT_FOUND");
        }

        List<QuoteLineItem> items = new ArrayList<>();
        BigDecimal dressPrice = money(dress.getPrice());
        items.add(QuoteLineItem.of(
                request.dress().productId().toString(),
                dressDescription(request.dress(), dress),
                1,
                dressPrice
        ));

        for (PrefabricatedProductQuoteRequest item : prefabricatedItems) {
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
                "COT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                resolveEventType(request.eventType()),
                request.deliveryDate(),
                findCustomerName(request.customerId()),
                request.dress().productId().toString(),
                dressDescription(request.dress(), dress),
                dressPrice,
                items,
                subtotal,
                LocalDateTime.now()
        );
    }

    /**
     * Builds the rendering model of an existing order quote.
     *
     * <p>The order is never modified and no payment data is required for the
     * rendering itself.</p>
     */
    public OrderQuote createOrderQuote(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new QuoteNotFoundException(orderId));

        List<QuoteLineItem> items = new ArrayList<>();
        if (order.getProducts() != null) {
            for (Product product : order.getProducts()) {
                if (product == null) {
                    continue;
                }
                int quantity = 1;
                if (product instanceof Card card) {
                    quantity = card.getQuantity();
                } else if (product instanceof Cup cup) {
                    quantity = cup.getQuantity();
                }
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

        return new OrderQuote(
                order.getOrderId(),
                order.getEventType(),
                order.getDeliveryDate(),
                null,
                items,
                total
        );
    }

    private String resolveEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        String normalized = eventType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_EVENT_TYPES.contains(normalized)) {
            throw new QuoteValidationException("EVENT_TYPE_INVALID");
        }
        return normalized;
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
            parts.add("Personalizaciones: " + dress.customizations());
        }
        if (dressProduct.getMaterials() != null && !dressProduct.getMaterials().isBlank()) {
            parts.add("Materiales: " + dressProduct.getMaterials());
        }
        if (parts.isEmpty()) {
            return "Vestido personalizado";
        }
        return String.join(", ", parts);
    }

    private String productDescription(Product product) {
        if (product.getMaterials() == null || product.getMaterials().isBlank()) {
            return "Producto " + product.getProductId();
        }
        return product.getMaterials();
    }
}
