package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.models.Order;
import com.keycode.ilussioness_backend.services.AppointmentService;
import com.keycode.ilussioness_backend.services.CustomerService;
import com.keycode.ilussioness_backend.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level security and input-validation checks for the business API.
 *
 * <p>Business endpoints now require authentication. Most tests run with a mock
 * authenticated principal because they focus on hostile input; the dedicated
 * anonymous tests below pin the {@code 401} rejection and the real JWT flow is
 * covered by {@code AuthApiTest}.</p>
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:business-api-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin@ilussiones.com", roles = "ADMIN")
class BusinessApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from order_products");
        jdbcTemplate.update("delete from appointment");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from customer");
    }

    // ------------------------------------------------------------------
    // SQL injection
    // ------------------------------------------------------------------

    @Test
    void sqlInjectionInJsonValueIsStoredAsInertData() throws Exception {
        String payload = "'; DROP TABLE customer;--";

        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "phone": "555"}
                                """.formatted(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(payload));

        assertThat(jdbcTemplate.queryForObject("select count(*) from customer", Long.class))
                .isEqualTo(1);
        assertThat(customerService.getAllCustomers())
                .extracting(Customer::getName)
                .containsExactly(payload);
    }

    @Test
    void sqlInjectionInUuidPathVariableIsRejected() throws Exception {
        mockMvc.perform(get("/api/customer/{id}", "' OR '1'='1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.reason").value("malformed_parameter"));
    }

    @Test
    void sqlInjectionInOrderPathVariableMatchesNothing() throws Exception {
        mockMvc.perform(get("/api/order/{id}", "' OR '1'='1"))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Long.class))
                .isZero();
    }

    // ------------------------------------------------------------------
    // Malformed input
    // ------------------------------------------------------------------

    @Test
    void malformedUuidPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/customer/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("malformed_parameter"));
    }

    @Test
    void malformedJsonBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Ana\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.reason").value("malformed_payload"));
    }

    @Test
    void blankCustomerNameIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "phone": "555"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.name").exists());

        assertThat(jdbcTemplate.queryForObject("select count(*) from customer", Long.class))
                .isZero();
    }

    @Test
    void missingPhoneIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.phone").exists());
    }

    @Test
    void oversizedCustomerNameIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "phone": "555"}
                                """.formatted("a".repeat(200))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void appointmentWithUnknownCustomerIsRejected() throws Exception {
        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {"id": "%s"},
                                  "reason": "fittings",
                                  "appointmentDate": "2026-03-01",
                                  "appointmentTime": "10:00:00"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("customer_not_found"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from appointment", Long.class))
                .isZero();
    }

    @Test
    void appointmentWithoutRequiredFieldsIsRejected() throws Exception {
        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "fittings"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void orderWithoutEventTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "ORDER-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    // ------------------------------------------------------------------
    // Not-found and contract behaviour
    // ------------------------------------------------------------------

    @Test
    void unknownIdsReturn404() throws Exception {
        mockMvc.perform(get("/api/customer/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/customer/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/customer/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "phone": "555"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerUpdateIsSupportedWithPut() throws Exception {
        Customer created = customerService.createCustomer(new Customer("Ana", "555"));

        mockMvc.perform(put("/api/customer/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana María", "phone": "556"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana María"));
    }

    // ------------------------------------------------------------------
    // Partial PUT contract (the frontend sends only the changed fields)
    // ------------------------------------------------------------------

    @Test
    void orderPartialUpdateAcceptsBodyWithOnlyEventType() throws Exception {
        Order created = orderService.createOrder(new Order("ORDER-PARTIAL", "boda", null, null));

        mockMvc.perform(put("/api/order/{id}", created.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType": "quinceanera"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-PARTIAL"))
                .andExpect(jsonPath("$.eventType").value("quinceanera"));
    }

    @Test
    void appointmentPartialUpdateAcceptsBodyWithOnlyReason() throws Exception {
        Customer customer = customerService.createCustomer(new Customer("Ana", "555"));
        Appointment created = appointmentService.createAppointment(new Appointment(
                customer, "fittings", LocalDate.of(2026, 3, 1), LocalTime.of(10, 0), null));

        mockMvc.perform(put("/api/appointment/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "final fitting"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("final fitting"));
    }

    @Test
    void customerPartialUpdateKeepsUnspecifiedFields() throws Exception {
        Customer created = customerService.createCustomer(new Customer("Ana", "555"));

        mockMvc.perform(put("/api/customer/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana López"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana López"))
                .andExpect(jsonPath("$.phone").value("555"));
    }

    @Test
    void errorResponsesDoNotLeakInternals() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "phone": "555"}
                                """))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .doesNotContain("org.hibernate")
                .doesNotContainIgnoringCase("select ")
                .doesNotContain("insert ");
    }

    // ------------------------------------------------------------------
    // Authentication boundary
    // ------------------------------------------------------------------

    @Test
    @WithAnonymousUser
    void businessEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/customer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));

        mockMvc.perform(get("/api/customer/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "phone": "555"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void invalidBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/customer")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithAnonymousUser
    void sqlInjectionInLoginDoesNotGrantAccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@ilussiones.com", "password": "' OR '1'='1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void authenticatedRequestsReachTheBusinessApi() throws Exception {
        mockMvc.perform(get("/api/customer"))
                .andExpect(status().isOk());
    }
}
