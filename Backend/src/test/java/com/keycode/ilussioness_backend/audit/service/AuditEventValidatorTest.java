package com.keycode.ilussioness_backend.audit.service;

import com.keycode.ilussioness_backend.audit.api.AuditEventRequest;
import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventValidatorTest {

    private final AuditEventValidator validator =
            new AuditEventValidator(JsonMapper.builder().build());

    @Test
    void acceptsValidFrontendEventAndCanonicalisesMetadata() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_LOGIN_SUBMITTED,
                AuditEntityType.AUTH,
                null,
                Map.<String, Object>of("AUTH_METHOD", "PASSWORD")
        );

        Map<String, Object> metadata = validator.validateClientEvent(request);

        assertThat(metadata).containsExactly(Map.entry("AUTH_METHOD", "PASSWORD"));
    }

    @Test
    void rejectsBusinessEventTypesFromTheClient() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.CUSTOMER_CREATED,
                AuditEntityType.CUSTOMER,
                null,
                Map.of()
        );

        assertValidationCode(request, "event_type_not_allowed");
    }

    @Test
    void rejectsEntityTypeThatDoesNotBelongToEventType() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_LOGIN_SUBMITTED,
                AuditEntityType.CUSTOMER,
                null,
                Map.of()
        );

        assertValidationCode(request, "entity_type_not_allowed");
    }

    @Test
    void rejectsUnknownMetadataKey() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_LOGIN_SUBMITTED,
                AuditEntityType.AUTH,
                null,
                Map.<String, Object>of("EMAIL", "customer@example.com")
        );

        assertValidationCode(request, "metadata_key_not_allowed");
    }

    @Test
    void rejectsMetadataKeyNotAllowedForEventType() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_SESSION_STARTED,
                AuditEntityType.AUTH,
                null,
                Map.<String, Object>of("AUTH_METHOD", "PASSWORD")
        );

        assertValidationCode(request, "metadata_key_not_allowed");
    }

    @Test
    void rejectsMetadataValueOutsideAllowedSet() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_SECTION_VIEWED,
                AuditEntityType.APPLICATION,
                null,
                Map.<String, Object>of("SECTION", "ADMIN")
        );

        assertValidationCode(request, "metadata_value_invalid");
    }

    @Test
    void rejectsNestedMetadata() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_SECTION_VIEWED,
                AuditEntityType.APPLICATION,
                null,
                Map.<String, Object>of("SECTION", List.of("INICIO"))
        );

        assertValidationCode(request, "metadata_value_invalid");
    }

    @Test
    void rejectsControlCharactersAndOversizedText() {
        AuditEventRequest controlCharacters = new AuditEventRequest(
                AuditEventType.UI_SECTION_VIEWED,
                AuditEntityType.APPLICATION,
                null,
                Map.<String, Object>of("SECTION", "INICIO\nCLIENTES")
        );

        assertValidationCode(controlCharacters, "metadata_value_invalid");

        AuditEventRequest oversized = new AuditEventRequest(
                AuditEventType.UI_UNEXPECTED_ERROR,
                AuditEntityType.APPLICATION,
                null,
                Map.<String, Object>of("ERROR_NAME", "A".repeat(121))
        );

        assertValidationCode(oversized, "metadata_value_invalid");
    }

    @Test
    void rejectsTooManyMetadataEntries() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int index = 0; index < 11; index++) {
            metadata.put("UNKNOWN_" + index, "value");
        }

        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_SESSION_STARTED,
                AuditEntityType.AUTH,
                null,
                metadata
        );

        assertValidationCode(request, "metadata_too_large");
    }

    @Test
    void rejectsInvalidEntityId() {
        AuditEventRequest invalidEntityId = new AuditEventRequest(
                AuditEventType.UI_APPOINTMENT_DETAIL_VIEWED,
                AuditEntityType.APPOINTMENT,
                "id with spaces",
                Map.of()
        );

        assertValidationCode(invalidEntityId, "entity_id_invalid");
    }

    @Test
    void rejectsHttpStatusOutsideTheValidRange() {
        AuditEventRequest request = new AuditEventRequest(
                AuditEventType.UI_API_REQUEST_FAILED,
                AuditEntityType.APPOINTMENT,
                null,
                Map.<String, Object>of(
                        "OPERATION", "LOAD_APPOINTMENTS",
                        "ERROR_CODE", "HTTP_ERROR",
                        "HTTP_STATUS", 700
                )
        );

        assertValidationCode(request, "metadata_value_invalid");
    }

    @Test
    void acceptsBusinessEventMetadataDocumentedForThatEvent() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("CUSTOMER_ID", "6f4f33bb-3e8e-4e62-9334-913ae4b90f0f");
        metadata.put("APPOINTMENT_STATUS", "SCHEDULED");

        Map<String, Object> sanitised = validator.validateBusinessEvent(
                AuditEventType.APPOINTMENT_CREATED,
                AuditEntityType.APPOINTMENT,
                metadata
        );

        assertThat(sanitised).isEqualTo(metadata);
    }

    @Test
    void rejectsFrontendEventTypeFromBusinessValidation() {
        assertThatThrownBy(() -> validator.validateBusinessEvent(
                AuditEventType.UI_SESSION_STARTED,
                AuditEntityType.AUTH,
                Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Frontend event type");
    }

    private void assertValidationCode(AuditEventRequest request, String expectedCode) {
        assertThatThrownBy(() -> validator.validateClientEvent(request))
                .isInstanceOf(AuditEventValidationException.class)
                .extracting(ex -> ((AuditEventValidationException) ex).getCode())
                .isEqualTo(expectedCode);
    }
}
