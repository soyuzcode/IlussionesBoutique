package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.AppointmentStatus;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.models.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-level business rules and edge cases against the isolated H2 database.
 *
 * <p>Every rejection must happen in the service (before persistence) and must
 * not leave a business row behind.</p>
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:business-service-rules;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class BusinessServiceRulesTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from order_products");
        jdbcTemplate.update("delete from appointment");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from customer");
    }

    // ------------------------------------------------------------------
    // Customer
    // ------------------------------------------------------------------

    @Test
    void customerCrudRoundTrip() {
        Customer created = customerService.createCustomer(new Customer("  Ana  ", " 555 "));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Ana");
        assertThat(created.getPhone()).isEqualTo("555");
        assertThat(customerService.getCustomerById(created.getId())).isPresent();
        assertThat(customerService.getAllCustomers()).hasSize(1);

        Customer updated = customerService.updateCustomer(
                created.getId(), new Customer("Ana María", "556")).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Ana María");

        assertThat(customerService.deleteCustomer(created.getId())).isTrue();
        assertThat(customerService.getCustomerById(created.getId())).isEmpty();
    }

    @Test
    void customerNameIsRequired() {
        assertThatThrownBy(() -> customerService.createCustomer(new Customer("  ", "555")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("name_required");
        assertThat(countRows("customer")).isZero();
    }

    @Test
    void customerPhoneIsRequired() {
        assertThatThrownBy(() -> customerService.createCustomer(new Customer("Ana", null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("phone_required");
        assertThat(countRows("customer")).isZero();
    }

    @Test
    void customerNameOverLimitIsRejected() {
        assertThatThrownBy(() -> customerService.createCustomer(
                new Customer("a".repeat(121), "555")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("name_too_long");
        assertThat(countRows("customer")).isZero();
    }

    @Test
    void customerPhoneOverLimitIsRejected() {
        assertThatThrownBy(() -> customerService.createCustomer(
                new Customer("Ana", "5".repeat(21))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("phone_too_long");
        assertThat(countRows("customer")).isZero();
    }

    @Test
    void customerUpdateOfUnknownIdReturnsEmpty() {
        assertThat(customerService.updateCustomer(UUID.randomUUID(), new Customer("Ana", "555")))
                .isEmpty();
    }

    @Test
    void customerDeleteOfUnknownIdReturnsFalse() {
        assertThat(customerService.deleteCustomer(UUID.randomUUID())).isFalse();
    }

    // ------------------------------------------------------------------
    // Appointment
    // ------------------------------------------------------------------

    @Test
    void appointmentCrudRoundTripAndCustomerLookup() {
        Customer customer = customerService.createCustomer(new Customer("Ana", "555"));
        Appointment created = appointmentService.createAppointment(new Appointment(
                customer, "fittings", LocalDate.of(2026, 2, 1), LocalTime.of(12, 0),
                AppointmentStatus.SCHEDULED));

        assertThat(created.getId()).isNotNull();
        assertThat(appointmentService.getAppointmentById(created.getId())).isPresent();
        assertThat(appointmentService.getAppointmentsByCustomer(customer.getId())).hasSize(1);

        Appointment updated = appointmentService.updateAppointment(
                created.getId(), new Appointment(null, "rescheduled", null, null, null))
                .orElseThrow();
        assertThat(updated.getReason()).isEqualTo("rescheduled");
        assertThat(updated.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 2, 1));

        assertThat(appointmentService.deleteAppointment(created.getId())).isTrue();
        assertThat(appointmentService.getAppointmentById(created.getId())).isEmpty();
    }

    @Test
    void appointmentDefaultsToScheduledWhenStatusMissing() {
        Customer customer = customerService.createCustomer(new Customer("Ana", "555"));

        Appointment created = appointmentService.createAppointment(new Appointment(
                customer, "fittings", LocalDate.now(), LocalTime.of(9, 0), null));

        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(jdbcTemplate.queryForObject(
                "select status from appointment where id = ?", String.class, created.getId()))
                .isEqualTo("SCHEDULED");
    }

    @Test
    void appointmentRequiresExistingCustomer() {
        Customer ghost = new Customer("Ana", "555");
        ghost.setId(UUID.randomUUID());

        assertThatThrownBy(() -> appointmentService.createAppointment(new Appointment(
                ghost, "fittings", LocalDate.now(), LocalTime.of(9, 0), null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("customer_not_found");
        assertThat(countRows("appointment")).isZero();
    }

    @Test
    void appointmentRequiresDateAndTime() {
        Customer customer = customerService.createCustomer(new Customer("Ana", "555"));

        assertThatThrownBy(() -> appointmentService.createAppointment(new Appointment(
                customer, "fittings", null, LocalTime.of(9, 0), null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("appointment_date_required");

        assertThatThrownBy(() -> appointmentService.createAppointment(new Appointment(
                customer, "fittings", LocalDate.now(), null, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("appointment_time_required");

        assertThat(countRows("appointment")).isZero();
    }

    @Test
    void appointmentReasonOverLimitIsRejected() {
        Customer customer = customerService.createCustomer(new Customer("Ana", "555"));

        assertThatThrownBy(() -> appointmentService.createAppointment(new Appointment(
                customer, "r".repeat(501), LocalDate.now(), LocalTime.of(9, 0), null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("reason_too_long");
        assertThat(countRows("appointment")).isZero();
    }

    @Test
    void appointmentUpdateOfUnknownIdReturnsEmpty() {
        assertThat(appointmentService.updateAppointment(UUID.randomUUID(), new Appointment()))
                .isEmpty();
    }

    @Test
    void appointmentDeleteOfUnknownIdReturnsFalse() {
        assertThat(appointmentService.deleteAppointment(UUID.randomUUID())).isFalse();
    }

    // ------------------------------------------------------------------
    // Order
    // ------------------------------------------------------------------

    @Test
    void orderCrudRoundTrip() {
        Order created = orderService.createOrder(
                new Order("ORDER-1", "SALE", new Date(), List.of()));

        assertThat(orderService.getOrderById("ORDER-1")).isPresent();
        assertThat(orderService.getAllOrders()).hasSize(1);

        Order updated = orderService.updateOrder(
                "ORDER-1", new Order(null, "WEDDING", null, null)).orElseThrow();
        assertThat(updated.getEventType()).isEqualTo("WEDDING");
        assertThat(updated.getDeliveryDate()).isNotNull();

        assertThat(orderService.deleteOrder("ORDER-1")).isTrue();
        assertThat(orderService.getOrderById("ORDER-1")).isEmpty();
        assertThat(created.getOrderId()).isEqualTo("ORDER-1");
    }

    @Test
    void orderEventTypeIsRequired() {
        assertThatThrownBy(() -> orderService.createOrder(new Order("ORDER-2", "  ", null, List.of())))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("event_type_required");
        assertThat(countRows("orders")).isZero();
    }

    @Test
    void orderIdOverLimitIsRejected() {
        assertThatThrownBy(() -> orderService.createOrder(
                new Order("o".repeat(65), "SALE", null, List.of())))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("order_id_too_long");
        assertThat(countRows("orders")).isZero();
    }

    @Test
    void orderUpdateOfUnknownIdReturnsEmpty() {
        assertThat(orderService.updateOrder("ORDER-404", new Order()))
                .isEmpty();
    }

    @Test
    void orderDeleteOfUnknownIdReturnsFalse() {
        assertThat(orderService.deleteOrder("ORDER-404")).isFalse();
    }

    private long countRows(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
