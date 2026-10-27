package com.keycode.ilussioness_backend.audit.service;

import com.keycode.ilussioness_backend.audit.api.AuditEventRequest;
import com.keycode.ilussioness_backend.audit.domain.AuditAction;
import com.keycode.ilussioness_backend.audit.domain.AuditActorType;
import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.domain.AuditSource;
import com.keycode.ilussioness_backend.audit.repository.AuditLogRepository;
import com.keycode.ilussioness_backend.audit.web.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:audit-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
@Import(AuditServicePersistenceTest.TransactionProbeConfig.class)
class AuditServicePersistenceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionProbe transactionProbe;

    @BeforeEach
    void cleanAuditTable() {
        jdbcTemplate.update("delete from audit_log");
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void frontendEventUsesServerDerivedIdentityAndContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                RequestIdFilter.SESSION_ID_ATTRIBUTE,
                "session-from-header"
        );
        request.setAttribute(
                RequestIdFilter.REQUEST_ID_ATTRIBUTE,
                "server-generated-request-id"
        );
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuditLog saved = auditService.recordClientEvent(new AuditEventRequest(
                AuditEventType.UI_LOGIN_SUBMITTED,
                AuditEntityType.AUTH,
                null,
                Map.<String, Object>of("AUTH_METHOD", "PASSWORD")
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.UI_LOGIN_SUBMITTED);
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(saved.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getEventSource()).isEqualTo(AuditSource.FRONTEND);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.INFO);
        assertThat(saved.getSessionId()).isEqualTo("session-from-header");
        assertThat(saved.getRequestId()).isEqualTo("server-generated-request-id");
        assertThat(saved.getMetadata()).containsEntry("AUTH_METHOD", "PASSWORD");
    }

    @Test
    void clientCannotProvideActorOutcomeSourceOrAction() {
        AuditLog saved = auditService.recordClientEvent(new AuditEventRequest(
                AuditEventType.UI_SESSION_STARTED,
                AuditEntityType.AUTH,
                null,
                Map.of()
        ));

        assertThat(saved.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getEventSource()).isEqualTo(AuditSource.FRONTEND);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.INFO);
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(auditLogRepository.count()).isEqualTo(1);
    }

    @Test
    void successAuditRequiresAnActiveBusinessTransaction() {
        assertThatThrownBy(() -> auditService.recordBusinessSuccess(
                AuditEventType.CUSTOMER_CREATED,
                AuditEntityType.CUSTOMER,
                "customer-id",
                Map.of()
        ))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void successAuditCommitsWithTheBusinessTransaction() {
        transactionProbe.createSuccessThenCommit();

        assertThat(auditLogRepository.count()).isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select event_type, outcome, event_source from audit_log"
        );
        assertThat(row)
                .containsEntry("EVENT_TYPE", "CUSTOMER_CREATED")
                .containsEntry("OUTCOME", "SUCCESS")
                .containsEntry("EVENT_SOURCE", "BACKEND");
    }

    @Test
    void failureEventMapsRejectionMetadataAndOutcome() {
        AuditLog saved = auditService.recordBusinessFailure(
                AuditEventType.CUSTOMER_CREATE_FAILED,
                AuditEntityType.CUSTOMER,
                "customer-id",
                AuditOutcome.FAILURE,
                Map.of("REASON", "PERSISTENCE_ERROR")
        );

        assertThat(saved.getEventType()).isEqualTo(AuditEventType.CUSTOMER_CREATE_FAILED);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(saved.getEventSource()).isEqualTo(AuditSource.BACKEND);
        assertThat(saved.getMetadata()).containsEntry("REASON", "PERSISTENCE_ERROR");
    }

    @Test
    void successAuditRollsBackWithTheBusinessTransaction() {
        assertThatThrownBy(transactionProbe::createSuccessThenRollback)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business rollback");

        assertThat(auditLogRepository.count()).isZero();
    }

    @Test
    void failureAuditSurvivesBusinessTransactionRollback() {
        assertThatThrownBy(transactionProbe::recordFailureThenRollback)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business rollback");

        assertThat(auditLogRepository.count()).isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select event_type, outcome from audit_log"
        );
        assertThat(row)
                .containsEntry("EVENT_TYPE", "CUSTOMER_CREATE_FAILED")
                .containsEntry("OUTCOME", "FAILURE");
    }

    @Test
    void failureAuditRejectsSuccessOutcome() {
        assertThatThrownBy(() -> auditService.recordBusinessFailure(
                AuditEventType.CUSTOMER_CREATE_FAILED,
                AuditEntityType.CUSTOMER,
                null,
                AuditOutcome.SUCCESS,
                Map.of("REASON", "PERSISTENCE_ERROR")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAILURE or REJECTED");

        assertThat(auditLogRepository.count()).isZero();
    }

    @TestConfiguration
    static class TransactionProbeConfig {

        @Bean
        TransactionProbe transactionProbe(AuditService auditService) {
            return new TransactionProbe(auditService);
        }
    }

    static class TransactionProbe {

        private final AuditService auditService;

        TransactionProbe(AuditService auditService) {
            this.auditService = auditService;
        }

        @Transactional
        public void createSuccessThenCommit() {
            auditService.recordBusinessSuccess(
                    AuditEventType.CUSTOMER_CREATED,
                    AuditEntityType.CUSTOMER,
                    "customer-id",
                    Map.of()
            );
        }

        @Transactional
        public void createSuccessThenRollback() {
            auditService.recordBusinessSuccess(
                    AuditEventType.CUSTOMER_CREATED,
                    AuditEntityType.CUSTOMER,
                    "customer-id",
                    Map.of()
            );
            throw new IllegalStateException("business rollback");
        }

        @Transactional
        public void recordFailureThenRollback() {
            auditService.recordBusinessFailure(
                    AuditEventType.CUSTOMER_CREATE_FAILED,
                    AuditEntityType.CUSTOMER,
                    null,
                    AuditOutcome.FAILURE,
                    Map.of("REASON", "PERSISTENCE_ERROR")
            );
            throw new IllegalStateException("business rollback");
        }
    }
}
