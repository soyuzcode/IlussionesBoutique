package com.keycode.ilussioness_backend.repositories;

import com.keycode.ilussioness_backend.audit.domain.AuditAction;
import com.keycode.ilussioness_backend.audit.domain.AuditActorType;
import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.domain.AuditSource;
import com.keycode.ilussioness_backend.audit.repository.AuditLogRepository;
import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.AppointmentStatus;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.models.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level persistence checks against the isolated H2 database.
 *
 * <p>These tests exercise the JPA mappings (UUID keys, enum columns, JSON
 * metadata, text order ids) without starting the web layer.</p>
 */
@DataJpaTest(properties =
        "spring.datasource.url=jdbc:h2:mem:business-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BusinessRepositoryJpaTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Customer persistCustomer(String name, String phone) {
        return customerRepository.saveAndFlush(new Customer(name, phone));
    }

    @Test
    void customerIsPersistedAndFoundById() {
        Customer saved = persistCustomer("Ana", "555");

        assertThat(saved.getId()).isNotNull();
        entityManager.clear();

        assertThat(customerRepository.findById(saved.getId()))
                .get()
                .extracting(Customer::getName, Customer::getPhone)
                .containsExactly("Ana", "555");
    }

    @Test
    void customerIsUpdatedInPlace() {
        Customer customer = persistCustomer("Ana", "555");
        customer.setName("Ana María");
        customer.setPhone("556");
        customerRepository.saveAndFlush(customer);
        entityManager.clear();

        Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Ana María");
        assertThat(reloaded.getPhone()).isEqualTo("556");
    }

    @Test
    void customerIsDeleted() {
        Customer customer = persistCustomer("Ana", "555");

        customerRepository.deleteById(customer.getId());
        customerRepository.flush();

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    void appointmentPersistsCustomerReferenceAndEnumStatus() {
        Customer customer = persistCustomer("Ana", "555");
        Appointment saved = appointmentRepository.saveAndFlush(new Appointment(
                customer,
                "fittings",
                LocalDate.of(2026, 1, 15),
                LocalTime.of(10, 30),
                AppointmentStatus.SCHEDULED
        ));
        entityManager.clear();

        Appointment found = appointmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(found.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(found.getReason()).isEqualTo("fittings");
        assertThat(found.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(found.getAppointmentTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void appointmentQueryFiltersByCustomer() {
        Customer first = persistCustomer("Ana", "555");
        Customer second = persistCustomer("Beto", "556");
        appointmentRepository.saveAndFlush(new Appointment(
                first, "one", LocalDate.now(), LocalTime.of(9, 0), AppointmentStatus.SCHEDULED));
        appointmentRepository.saveAndFlush(new Appointment(
                second, "two", LocalDate.now(), LocalTime.of(11, 0), AppointmentStatus.SCHEDULED));

        List<Appointment> firstCustomerAppointments =
                appointmentRepository.findByCustomerId(first.getId());

        assertThat(firstCustomerAppointments).hasSize(1);
        assertThat(firstCustomerAppointments.get(0).getCustomer().getId())
                .isEqualTo(first.getId());
    }

    @Test
    void orderPersistsWithTextIdentifier() {
        orderRepository.saveAndFlush(new Order("ORDER-100", "SALE", new Date(), List.of()));
        entityManager.clear();

        assertThat(orderRepository.findById("ORDER-100")).isPresent();
    }

    @Test
    void orderIsUpdated() {
        orderRepository.saveAndFlush(new Order("ORDER-200", "SALE", null, List.of()));
        Order stored = orderRepository.findById("ORDER-200").orElseThrow();
        stored.setEventType("WEDDING");
        stored.setDeliveryDate(new Date());
        orderRepository.saveAndFlush(stored);
        entityManager.clear();

        Order reloaded = orderRepository.findById("ORDER-200").orElseThrow();
        assertThat(reloaded.getEventType()).isEqualTo("WEDDING");
        assertThat(reloaded.getDeliveryDate()).isNotNull();
    }

    @Test
    void orderIsDeleted() {
        orderRepository.saveAndFlush(new Order("ORDER-300", "SALE", null, List.of()));

        orderRepository.deleteById("ORDER-300");
        orderRepository.flush();

        assertThat(orderRepository.findById("ORDER-300")).isEmpty();
    }

    @Test
    void auditLogPersistsServerGeneratedFieldsAndJsonMetadata() {
        AuditLog event = new AuditLog();
        event.setEventType(AuditEventType.CUSTOMER_CREATED);
        event.setAction(AuditAction.CREATE);
        event.setActorType(AuditActorType.ANONYMOUS);
        event.setEntityType(AuditEntityType.CUSTOMER);
        event.setEventSource(AuditSource.BACKEND);
        event.setOutcome(AuditOutcome.SUCCESS);
        event.setMetadata(Map.of("REASON", "OK"));

        AuditLog saved = auditLogRepository.save(event);
        entityManager.flush();
        entityManager.clear();

        AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOccurredAt()).isNotNull();
        assertThat(found.getRequestId()).isNotBlank();
        assertThat(found.getMetadata()).containsEntry("REASON", "OK");
        assertThat(auditLogRepository.count()).isEqualTo(1);
    }
}
