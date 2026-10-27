package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer.getStatus() == null) {
            customer.setStatus(com.keycode.ilussioness_backend.models.CustomerStatus.ACTIVO);
        }
        Customer saved;
        try {
            saved = customerRepository.saveAndFlush(customer);
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.CUSTOMER_CREATE_FAILED,
                    null,
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.CUSTOMER_CREATED,
                AuditEntityType.CUSTOMER,
                saved.getId() == null ? null : saved.getId().toString(),
                Map.of()
        );
        return saved;
    }

    /**
     * Updates the mutable customer fields and records the outcome.
     *
     * @return the updated customer, or empty when the customer did not exist.
     */
    @Transactional
    public Optional<Customer> updateCustomer(UUID id, Customer updatedCustomer) {
        Optional<Customer> existing = customerRepository.findById(id);
        if (existing.isEmpty()) {
            auditFailure(
                    AuditEventType.CUSTOMER_UPDATE_REJECTED,
                    id.toString(),
                    AuditOutcome.REJECTED,
                    "NOT_FOUND"
            );
            return Optional.empty();
        }

        Customer customer = existing.get();
        if (updatedCustomer.getName() != null) {
            customer.setName(updatedCustomer.getName());
        }
        if (updatedCustomer.getPhone() != null) {
            customer.setPhone(updatedCustomer.getPhone());
        }
        if (updatedCustomer.getStatus() != null) {
            customer.setStatus(updatedCustomer.getStatus());
        }

        Customer saved;
        try {
            saved = customerRepository.saveAndFlush(customer);
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.CUSTOMER_UPDATE_FAILED,
                    id.toString(),
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.CUSTOMER_UPDATED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                Map.of()
        );
        return Optional.of(saved);
    }

    /**
     * @return true when deleted, false when the customer did not exist.
     */
    @Transactional
    public boolean deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id)) {
            auditFailure(
                    AuditEventType.CUSTOMER_DELETE_REJECTED,
                    id.toString(),
                    AuditOutcome.REJECTED,
                    "NOT_FOUND"
            );
            return false;
        }

        try {
            customerRepository.deleteById(id);
            customerRepository.flush();
        } catch (RuntimeException ex) {
            auditFailure(
                    AuditEventType.CUSTOMER_DELETE_FAILED,
                    id.toString(),
                    AuditOutcome.FAILURE,
                    "PERSISTENCE_ERROR"
            );
            throw ex;
        }

        auditService.recordBusinessSuccess(
                AuditEventType.CUSTOMER_DELETED,
                AuditEntityType.CUSTOMER,
                id.toString(),
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
                    AuditEntityType.CUSTOMER,
                    entityId,
                    outcome,
                    Map.of("REASON", reason)
            );
        } catch (RuntimeException auditException) {
            // Never replace the original business exception with an audit error.
            LOG.warn("Could not persist customer audit failure {} ({})",
                    eventType, auditException.getClass().getSimpleName());
        }
    }
}
