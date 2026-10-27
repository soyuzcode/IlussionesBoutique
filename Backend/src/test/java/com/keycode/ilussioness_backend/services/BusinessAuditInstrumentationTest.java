package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.AppointmentStatus;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.models.Order;
import com.keycode.ilussioness_backend.repositories.AppointmentRepository;
import com.keycode.ilussioness_backend.repositories.CustomerRepository;
import com.keycode.ilussioness_backend.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessAuditInstrumentationTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuditService auditService;

    @Test
    void customerCreateWritesSuccessAuditWithSavedId() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        Customer input = new Customer("Ana", "555");
        Customer saved = new Customer("Ana", "555");
        saved.setId(UUID.randomUUID());
        when(customerRepository.saveAndFlush(input)).thenReturn(saved);

        Customer result = service.createCustomer(input);

        assertThat(result).isSameAs(saved);
        verify(auditService).recordBusinessSuccess(
                AuditEventType.CUSTOMER_CREATED,
                AuditEntityType.CUSTOMER,
                saved.getId().toString(),
                Map.of()
        );
    }

    @Test
    void customerCreateFailureWritesFailureAuditAndPreservesOriginalException() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        Customer input = new Customer("Ana", "555");
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("database is down");
        when(customerRepository.saveAndFlush(input)).thenThrow(businessException);

        assertThatThrownBy(() -> service.createCustomer(input))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.CUSTOMER_CREATE_FAILED,
                AuditEntityType.CUSTOMER,
                null,
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }

    @Test
    void customerCreateFailureIsNotMaskedWhenAuditAlsoFails() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        Customer input = new Customer("Ana", "555");
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("database is down");
        when(customerRepository.saveAndFlush(input)).thenThrow(businessException);
        doThrow(new IllegalStateException("audit store down"))
                .when(auditService)
                .recordBusinessFailure(any(), any(), any(), any(), anyMap());

        assertThatThrownBy(() -> service.createCustomer(input))
                .isSameAs(businessException);
    }

    @Test
    void customerCreateFailsWhenSuccessAuditCannotBeWritten() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        Customer input = new Customer("Ana", "555");
        Customer saved = new Customer("Ana", "555");
        saved.setId(UUID.randomUUID());
        when(customerRepository.saveAndFlush(input)).thenReturn(saved);
        doThrow(new IllegalStateException("audit store down"))
                .when(auditService)
                .recordBusinessSuccess(any(), any(), any(), anyMap());

        assertThatThrownBy(() -> service.createCustomer(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit store down");
    }

    @Test
    void customerUpdateWritesSuccessAuditWithCustomerId() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        UUID id = UUID.randomUUID();
        Customer existing = new Customer("Ana", "555");
        existing.setId(id);
        Customer changes = new Customer("Ana María", "556");
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        Optional<Customer> result = service.updateCustomer(id, changes);

        assertThat(result).containsSame(existing);
        assertThat(existing.getName()).isEqualTo("Ana María");
        assertThat(existing.getPhone()).isEqualTo("556");
        verify(auditService).recordBusinessSuccess(
                AuditEventType.CUSTOMER_UPDATED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                Map.of()
        );
    }

    @Test
    void customerUpdateOfMissingEntityWritesRejectedAudit() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Customer> result = service.updateCustomer(id, new Customer("Ana", "555"));

        assertThat(result).isEmpty();
        verify(customerRepository, never()).saveAndFlush(any(Customer.class));
        verify(auditService).recordBusinessFailure(
                AuditEventType.CUSTOMER_UPDATE_REJECTED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                AuditOutcome.REJECTED,
                Map.of("REASON", "NOT_FOUND")
        );
    }

    @Test
    void customerUpdateFailureWritesFailureAuditAndPreservesOriginalException() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        UUID id = UUID.randomUUID();
        Customer existing = new Customer("Ana", "555");
        existing.setId(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("constraint");
        when(customerRepository.saveAndFlush(existing)).thenThrow(businessException);

        assertThatThrownBy(() -> service.updateCustomer(id, new Customer("Ana", "555")))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.CUSTOMER_UPDATE_FAILED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }

    @Test
    void customerDeleteOfMissingEntityWritesRejectedAudit() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(false);

        boolean deleted = service.deleteCustomer(id);

        assertThat(deleted).isFalse();
        verify(customerRepository, never()).deleteById(id);
        verify(auditService).recordBusinessFailure(
                AuditEventType.CUSTOMER_DELETE_REJECTED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                AuditOutcome.REJECTED,
                Map.of("REASON", "NOT_FOUND")
        );
    }

    @Test
    void customerDeleteFailureWritesFailureAuditAndPreservesOriginalException() {
        CustomerService service = new CustomerService(customerRepository, auditService);
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(true);
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("foreign key");
        doThrow(businessException).when(customerRepository).flush();

        assertThatThrownBy(() -> service.deleteCustomer(id))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.CUSTOMER_DELETE_FAILED,
                AuditEntityType.CUSTOMER,
                id.toString(),
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }

    @Test
    void appointmentCreateWritesNonSensitiveBusinessMetadata() {
        AppointmentService service = new AppointmentService(appointmentRepository, auditService);
        Customer customer = new Customer("Ana", "555");
        customer.setId(UUID.randomUUID());
        Appointment appointment = new Appointment(
                customer,
                "fittings",
                LocalDate.now(),
                LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED
        );
        appointment.setId(UUID.randomUUID());
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenReturn(appointment);

        Appointment result = service.createAppointment(appointment);

        assertThat(result).isSameAs(appointment);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metadata =
                ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordBusinessSuccess(
                eq(AuditEventType.APPOINTMENT_CREATED),
                eq(AuditEntityType.APPOINTMENT),
                eq(appointment.getId().toString()),
                metadata.capture()
        );
        assertThat(metadata.getValue())
                .containsEntry("CUSTOMER_ID", customer.getId().toString())
                .containsEntry("APPOINTMENT_STATUS", "SCHEDULED")
                .doesNotContainKeys("NAME", "PHONE", "REASON", "TIME", "DATE");
    }

    @Test
    void appointmentCreateFailureWritesFailureAudit() {
        AppointmentService service = new AppointmentService(appointmentRepository, auditService);
        Appointment appointment = new Appointment();
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("missing customer");
        when(appointmentRepository.saveAndFlush(appointment)).thenThrow(businessException);

        assertThatThrownBy(() -> service.createAppointment(appointment))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.APPOINTMENT_CREATE_FAILED,
                AuditEntityType.APPOINTMENT,
                null,
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }

    @Test
    void orderCreateWritesAggregateMetadataOnly() {
        OrderService service = new OrderService(orderRepository, auditService);
        Order order = new Order(
                "ORDER-1",
                "SALE",
                new Date(),
                List.of()
        );
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        Order result = service.createOrder(order);

        assertThat(result).isSameAs(order);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metadata =
                ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordBusinessSuccess(
                eq(AuditEventType.ORDER_CREATED),
                eq(AuditEntityType.ORDER),
                eq("ORDER-1"),
                metadata.capture()
        );
        assertThat(metadata.getValue())
                .containsEntry("PRODUCT_COUNT", 0)
                .containsEntry("HAS_DELIVERY_DATE", true)
                .doesNotContainKeys("ORDER_ID", "EVENT_TYPE", "DELIVERY_DATE");
    }

    @Test
    void orderCreateFailureWritesFailureAudit() {
        OrderService service = new OrderService(orderRepository, auditService);
        Order order = new Order("ORDER-1", "SALE", null, List.of());
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("constraint");
        when(orderRepository.saveAndFlush(order)).thenThrow(businessException);

        assertThatThrownBy(() -> service.createOrder(order))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.ORDER_CREATE_FAILED,
                AuditEntityType.ORDER,
                null,
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }

    @Test
    void orderDeleteOfMissingEntityWritesRejectedAudit() {
        OrderService service = new OrderService(orderRepository, auditService);
        when(orderRepository.existsById("ORDER-404")).thenReturn(false);

        boolean deleted = service.deleteOrder("ORDER-404");

        assertThat(deleted).isFalse();
        verify(orderRepository, never()).deleteById("ORDER-404");
        verify(auditService).recordBusinessFailure(
                AuditEventType.ORDER_DELETE_REJECTED,
                AuditEntityType.ORDER,
                "ORDER-404",
                AuditOutcome.REJECTED,
                Map.of("REASON", "NOT_FOUND")
        );
    }

    @Test
    void orderDeleteFailureWritesFailureAuditAndPreservesOriginalException() {
        OrderService service = new OrderService(orderRepository, auditService);
        when(orderRepository.existsById("ORDER-1")).thenReturn(true);
        DataIntegrityViolationException businessException =
                new DataIntegrityViolationException("constraint");
        doThrow(businessException).when(orderRepository).flush();

        assertThatThrownBy(() -> service.deleteOrder("ORDER-1"))
                .isSameAs(businessException);

        verify(auditService).recordBusinessFailure(
                AuditEventType.ORDER_DELETE_FAILED,
                AuditEntityType.ORDER,
                "ORDER-1",
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );
    }
}
