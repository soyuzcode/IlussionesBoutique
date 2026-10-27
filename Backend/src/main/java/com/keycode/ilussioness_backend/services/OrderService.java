package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Order;
import com.keycode.ilussioness_backend.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Order createOrder(Order order) {
        Order saved;
        try {
            saved = orderRepository.saveAndFlush(order);
        } catch (RuntimeException ex) {
            auditFailure(AuditEventType.ORDER_CREATE_FAILED, null, AuditOutcome.FAILURE, "PERSISTENCE_ERROR");
            throw ex;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        int productCount = saved.getProducts() == null ? 0 : saved.getProducts().size();
        metadata.put("PRODUCT_COUNT", Math.min(productCount, 10_000));
        metadata.put("HAS_DELIVERY_DATE", saved.getDeliveryDate() != null);

        auditService.recordBusinessSuccess(
                AuditEventType.ORDER_CREATED,
                AuditEntityType.ORDER,
                saved.getOrderId(),
                metadata
        );
        return saved;
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
