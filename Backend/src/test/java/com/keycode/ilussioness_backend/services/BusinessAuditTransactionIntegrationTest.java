package com.keycode.ilussioness_backend.services;

import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.domain.AuditSource;
import com.keycode.ilussioness_backend.audit.repository.AuditLogRepository;
import com.keycode.ilussioness_backend.models.Appointment;
import com.keycode.ilussioness_backend.models.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:business-audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class BusinessAuditTransactionIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from appointment");
        jdbcTemplate.update("delete from customer");
    }

    @Test
    void successfulCustomerCreateCommitsBusinessRowAndSuccessAuditTogether() {
        Customer saved = customerService.createCustomer(new Customer("Ana", "555"));

        assertThat(saved.getId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from customer where id = ?",
                Long.class,
                saved.getId()
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where event_type = 'CUSTOMER_CREATED' and outcome = 'SUCCESS' and entity_id = ?",
                Long.class,
                saved.getId().toString()
        )).isEqualTo(1);
    }

    @Test
    void successfulCustomerUpdateCommitsBusinessRowAndSuccessAuditTogether() {
        Customer created = customerService.createCustomer(new Customer("Ana", "555"));

        Customer updated = customerService.updateCustomer(
                created.getId(),
                new Customer("Ana María", "556")
        ).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Ana María");
        assertThat(updated.getPhone()).isEqualTo("556");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from customer where id = ? and name = ?",
                Long.class,
                created.getId(),
                "Ana María"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where event_type = 'CUSTOMER_UPDATED' and outcome = 'SUCCESS' and entity_id = ?",
                Long.class,
                created.getId().toString()
        )).isEqualTo(1);
    }

    @Test
    void realPersistenceFailureIsAuditedAndOriginalExceptionIsReturned() {
        Appointment invalidAppointment = new Appointment();

        assertThatThrownBy(() -> appointmentService.createAppointment(invalidAppointment))
                .isInstanceOf(RuntimeException.class);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select id, event_type, outcome, event_source from audit_log"
        );
        assertThat(row)
                .containsEntry("EVENT_TYPE", "APPOINTMENT_CREATE_FAILED")
                .containsEntry("OUTCOME", "FAILURE")
                .containsEntry("EVENT_SOURCE", "BACKEND");

        AuditLog audit = auditLogRepository.findById(
                UUID.fromString(row.get("ID").toString())
        ).orElseThrow();
        assertThat(audit.getEventType()).isEqualTo(AuditEventType.APPOINTMENT_CREATE_FAILED);
        assertThat(audit.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(audit.getEventSource()).isEqualTo(AuditSource.BACKEND);
        assertThat(audit.getMetadata()).containsEntry("REASON", "PERSISTENCE_ERROR");
        assertThat(jdbcTemplate.queryForObject("select count(*) from appointment", Long.class))
                .isZero();
    }
}
