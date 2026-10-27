package com.keycode.ilussioness_backend.audit.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:audit-ingestion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAuditTable() {
        jdbcTemplate.update("delete from audit_log");
    }

    @Test
    void acceptsValidFrontendEventAndPersistsServerContext() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Audit-Session-Id", "session-12345678")
                        .header("X-Request-Id", "client-supplied-id")
                        .content("""
                                {
                                  "eventType": "UI_SECTION_VIEWED",
                                  "entityType": "APPLICATION",
                                  "metadata": {"SECTION": "INICIO"}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.not("client-supplied-id")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.occurredAt").exists());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select event_type, actor_type, event_source, outcome, action, session_id, request_id from audit_log"
        );

        assertThat(row)
                .containsEntry("EVENT_TYPE", "UI_SECTION_VIEWED")
                .containsEntry("ACTOR_TYPE", "ANONYMOUS")
                .containsEntry("EVENT_SOURCE", "FRONTEND")
                .containsEntry("OUTCOME", "INFO")
                .containsEntry("ACTION", "NAVIGATE")
                .containsEntry("SESSION_ID", "session-12345678");
        assertThat(row.get("REQUEST_ID"))
                .asString()
                .isNotBlank()
                .isNotEqualTo("client-supplied-id");
    }

    @Test
    void rejectsBusinessEventTypeFromClient() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CUSTOMER_CREATED",
                                  "entityType": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_audit_event"))
                .andExpect(jsonPath("$.reason").value("event_type_not_allowed"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_log", Long.class))
                .isZero();
    }

    @Test
    void rejectsUnknownField() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "UI_SESSION_STARTED",
                                  "entityType": "AUTH",
                                  "actorId": "forged-actor"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_audit_event"));
    }

    @Test
    void rejectsSessionIdInBodyBecauseCorrelationComesFromHeader() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "UI_SESSION_STARTED",
                                  "entityType": "AUTH",
                                  "sessionId": "session-12345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_audit_event"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_log", Long.class))
                .isZero();
    }

    @Test
    void rejectsUnknownMetadataKey() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "UI_LOGIN_SUBMITTED",
                                  "entityType": "AUTH",
                                  "metadata": {"EMAIL": "customer@example.com"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("metadata_key_not_allowed"));
    }

    @Test
    void rejectsOversizedBody() throws Exception {
        String oversized = """
                {
                  "eventType": "UI_SESSION_STARTED",
                  "entityType": "AUTH",
                  "entityId": "%s"
                }
                """.formatted("a".repeat(5000));

        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.error").value("payload_too_large"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_log", Long.class))
                .isZero();
    }

    @Test
    void exposesNoAuditReadEndpoint() throws Exception {
        mockMvc.perform(get("/api/audit/events"))
                .andExpect(status().isMethodNotAllowed());

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_log", Long.class))
                .isZero();
    }
}
