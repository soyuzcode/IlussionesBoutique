package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Order;
import com.keycode.ilussioness_backend.models.Product;
import com.keycode.ilussioness_backend.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);
    private static final int ORDER_ID_MAX_LENGTH = 64;

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrder(Order order) {
        try {
            requireValidOrder(order);
        } catch (BusinessValidationException ex) {
            auditFailure(
                    AuditEventType.ORDER_CREATE_FAILED,
                    order == null ? null : order.getOrderId(),
                    AuditOutcome.REJECTED,
                    BusinessValidationException.auditReason(ex.getCode())
            );
            throw ex;
        }

        Order saved;
        try {
            saved = orderRepository.saveAndFlush(order);
        } catch (RuntimeException ex) {
            auditFailure(AuditEventType.ORDER_CREATE_FAILED, null, AuditOutcome.FAILURE, "PERSISTENCE_ERROR");
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.ORDER_CREATED,
                AuditEntityType.ORDER,
                saved.getOrderId(),
                buildMetadata(saved)
        );
        return saved;
    }

    /**
     * Applies the non-null fields of {@code updates} to the stored order.
     *
     * @return the updated order, or empty when it did not exist.
     */
    @Transactional
    public Optional<Order> updateOrder(String id, Order updates) {
        if (id == null || id.isBlank()) {
            throw new BusinessValidationException("id_required");
        }
        Optional<Order> existing = orderRepository.findById(id);
        if (existing.isEmpty()) {
            auditFailure(AuditEventType.ORDER_UPDATE_REJECTED, id, AuditOutcome.REJECTED, "NOT_FOUND");
            return Optional.empty();
        }

        Order order = existing.get();
        if (updates != null) {
            if (updates.getEventType() != null) {
                order.setEventType(updates.getEventType());
            }
            if (updates.getDeliveryDate() != null) {
                order.setDeliveryDate(updates.getDeliveryDate());
            }
            if (updates.getProducts() != null) {
                order.setProducts(updates.getProducts());
            }
        }

        try {
            requireValidOrder(order);
        } catch (BusinessValidationException ex) {
            auditFailure(
                    AuditEventType.ORDER_UPDATE_REJECTED,
                    id,
                    AuditOutcome.REJECTED,
                    BusinessValidationException.auditReason(ex.getCode())
            );
            throw ex;
        }

        Order saved;
        try {
            saved = orderRepository.saveAndFlush(order);
        } catch (RuntimeException ex) {
            auditFailure(AuditEventType.ORDER_UPDATE_FAILED, id, AuditOutcome.FAILURE, "PERSISTENCE_ERROR");
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.ORDER_UPDATED,
                AuditEntityType.ORDER,
                id,
                buildMetadata(saved)
        );
        return Optional.of(saved);
    }

    /**
     * @return true when deleted, false when the order did not exist.
     */
    @Transactional
    public boolean deleteOrder(String id) {
        if (!orderRepository.existsById(id)) {
            auditFailure(AuditEventType.ORDER_DELETE_REJECTED, id, AuditOutcome.REJECTED, "NOT_FOUND");
            return false;
        }

        try {
            orderRepository.deleteById(id);
            orderRepository.flush();
        } catch (RuntimeException ex) {
            auditFailure(AuditEventType.ORDER_DELETE_FAILED, id, AuditOutcome.FAILURE, "PERSISTENCE_ERROR");
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.ORDER_DELETED,
                AuditEntityType.ORDER,
                id,
                Map.of()
        );
        return true;
    }

    private static Map<String, Object> buildMetadata(Order order) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        int productCount = order.getProducts() == null ? 0 : order.getProducts().size();
        metadata.put("PRODUCT_COUNT", Math.min(productCount, 10_000));
        metadata.put("HAS_DELIVERY_DATE", order.getDeliveryDate() != null);
        return metadata;
    }

    private static void requireValidOrder(Order order) {
        if (order == null) {
            throw new BusinessValidationException("payload_required");
        }
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            throw new BusinessValidationException("order_id_required");
        }
        if (order.getOrderId().length() > ORDER_ID_MAX_LENGTH) {
            throw new BusinessValidationException("order_id_too_long");
        }
        if (order.getEventType() == null || order.getEventType().isBlank()) {
            throw new BusinessValidationException("event_type_required");
        }
        if (order.getProducts() != null) {
            for (Product product : order.getProducts()) {
                if (product == null) {
                    throw new BusinessValidationException("invalid_product");
                }
                BigDecimal price = product.getPrice();
                if (price != null && price.signum() < 0) {
                    throw new BusinessValidationException("negative_price");
                }
            }
        }
    }

    private void auditFailure(
            AuditEventType eventType,
            String entityId,
            AuditOutcome outcome,
            String reason
    ) {
        try {
            auditService.recordBusinessFailure(
                    eventType,
                    AuditEntityType.ORDER,
                    entityId,
                    outcome,
                    Map.of("REASON", reason)
            );
        } catch (RuntimeException auditException) {
            LOG.warn("Could not persist order audit failure {} ({})",
                    eventType, auditException.getClass().getSimpleName());
        }
    }
}
