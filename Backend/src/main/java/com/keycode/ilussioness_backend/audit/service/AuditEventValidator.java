package com.keycode.ilussioness_backend.audit.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.keycode.ilussioness_backend.audit.api.AuditEventRequest;
import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditMetadataKey;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates and sanitises the bounded audit payload.
 *
 * <p>The validator is the single authority for the accepted metadata surface.
 * Unknown keys, nested structures, control characters, oversized values and
 * values outside the documented enum/range are rejected without echoing the
 * offending content.</p>
 */
@Component
public class AuditEventValidator {

    private static final int MAX_METADATA_ENTRIES = 10;
    private static final int MAX_METADATA_JSON_BYTES = 2048;
    private static final int MAX_STRING_LENGTH = 120;

    private static final Pattern SAFE_TEXT = Pattern.compile("^[\\p{L}\\p{N} .,:_/@+-]{1,120}$");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private static final Pattern ERROR_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,39}$");
    private static final Pattern CUSTOMER_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    private static final Set<String> SECTIONS = Set.of(
            "INICIO", "CLIENTES", "PEDIDOS", "INVENTARIO"
    );
    private static final Set<String> OPERATIONS = Set.of(
            "LOAD_APPOINTMENTS",
            "LOAD_CUSTOMERS",
            "CREATE_CUSTOMER",
            "CREATE_APPOINTMENT",
            "RELOAD_APPOINTMENTS",
            "LOAD_ORDERS",
            "CREATE_ORDER",
            "DELETE_ORDER",
            "DELETE_CUSTOMER"
    );
    private static final Set<String> ERROR_CODES = Set.of(
            "NETWORK_ERROR",
            "HTTP_ERROR",
            "TIMEOUT",
            "INVALID_RESPONSE",
            "VALIDATION_ERROR",
            "UNKNOWN_ERROR"
    );
    private static final Set<String> FIELDS = Set.of(
            "CLIENT", "TIME", "PERIOD", "TYPE"
    );
    private static final Set<String> REASONS = Set.of(
            "REQUIRED",
            "FORMAT",
            "NOT_FOUND",
            "INVALID_DATA",
            "PERSISTENCE_ERROR",
            "DUPLICATE",
            "DATABASE_ERROR"
    );
    private static final Set<String> AUTH_METHODS = Set.of(
            "PASSWORD", "GOOGLE"
    );
    private static final Set<String> APPOINTMENT_STATUSES = Set.of(
            "SCHEDULED", "COMPLETED", "CANCELLED"
    );

    private final ObjectMapper objectMapper;

    public AuditEventValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a frontend event and returns a canonical metadata map.
     */
    public Map<String, Object> validateClientEvent(AuditEventRequest request) {
        if (request == null) {
            throw invalid("payload_required");
        }

        AuditEventType eventType = request.eventType();
        if (eventType == null) {
            throw invalid("event_type_required");
        }
        if (!eventType.isClientIngestible()) {
            throw invalid("event_type_not_allowed");
        }

        AuditEntityType entityType = request.entityType();
        if (entityType == null || !eventType.getAllowedEntityTypes().contains(entityType)) {
            throw invalid("entity_type_not_allowed");
        }

        if (request.entityId() != null && !SAFE_IDENTIFIER.matcher(request.entityId()).matches()) {
            throw invalid("entity_id_invalid");
        }

        return sanitiseMetadata(eventType, request.metadata());
    }

    /**
     * Validates a server-generated business event. Invalid calls indicate a
     * programming error and abort the business transaction.
     */
    public Map<String, Object> validateBusinessEvent(
            AuditEventType eventType,
            AuditEntityType entityType,
            Map<String, Object> metadata
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("Audit event type is required");
        }
        if (eventType.isClientIngestible()) {
            throw new IllegalArgumentException("Frontend event type used for a business event");
        }
        if (entityType == null || !eventType.getAllowedEntityTypes().contains(entityType)) {
            throw new IllegalArgumentException("Entity type is not allowed for audit event " + eventType);
        }
        return sanitiseMetadata(eventType, metadata);
    }

    private Map<String, Object> sanitiseMetadata(
            AuditEventType eventType,
            Map<String, Object> metadata
    ) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        if (metadata.size() > MAX_METADATA_ENTRIES) {
            throw invalid("metadata_too_large");
        }

        Map<String, Object> sanitised = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String rawKey = entry.getKey();
            if (rawKey == null) {
                throw invalid("metadata_key_invalid");
            }

            AuditMetadataKey key;
            try {
                key = AuditMetadataKey.valueOf(rawKey);
            } catch (IllegalArgumentException ex) {
                throw invalid("metadata_key_not_allowed");
            }

            if (!eventType.getAllowedMetadataKeys().contains(key)) {
                throw invalid("metadata_key_not_allowed");
            }

            sanitised.put(key.name(), sanitiseValue(key, entry.getValue()));
        }

        try {
            if (objectMapper.writeValueAsBytes(sanitised).length > MAX_METADATA_JSON_BYTES) {
                throw invalid("metadata_too_large");
            }
        } catch (JacksonException ex) {
            throw invalid("metadata_invalid");
        }

        return sanitised;
    }

    private Object sanitiseValue(AuditMetadataKey key, Object value) {
        if (value == null) {
            throw invalid("metadata_value_invalid");
        }

        if (value instanceof Boolean booleanValue) {
            if (key != AuditMetadataKey.HAS_DELIVERY_DATE) {
                throw invalid("metadata_value_invalid");
            }
            return booleanValue;
        }

        if (value instanceof Number number) {
            Integer integerValue = toInteger(number);
            if (integerValue == null) {
                throw invalid("metadata_value_invalid");
            }
            return switch (key) {
                case HTTP_STATUS -> {
                    if (integerValue < 100 || integerValue > 599) {
                        throw invalid("metadata_value_invalid");
                    }
                    yield integerValue;
                }
                case PRODUCT_COUNT -> {
                    if (integerValue < 0 || integerValue > 10_000) {
                        throw invalid("metadata_value_invalid");
                    }
                    yield integerValue;
                }
                default -> throw invalid("metadata_value_invalid");
            };
        }

        if (!(value instanceof String stringValue)) {
            // Rejects maps, lists, arrays and other nested/unexpected values.
            throw invalid("metadata_value_invalid");
        }

        String text = stringValue.trim();
        if (text.isEmpty() || text.length() > MAX_STRING_LENGTH) {
            throw invalid("metadata_value_invalid");
        }

        if (key == AuditMetadataKey.CUSTOMER_ID) {
            if (!CUSTOMER_ID.matcher(text).matches()) {
                throw invalid("metadata_value_invalid");
            }
            return text;
        }

        if (key == AuditMetadataKey.ERROR_NAME) {
            if (!ERROR_NAME.matcher(text).matches()) {
                throw invalid("metadata_value_invalid");
            }
            return text;
        }

        if (!SAFE_TEXT.matcher(text).matches()) {
            throw invalid("metadata_value_invalid");
        }

        return switch (key) {
            case SECTION -> requireOneOf(SECTIONS, text);
            case OPERATION -> requireOneOf(OPERATIONS, text);
            case ERROR_CODE -> requireOneOf(ERROR_CODES, text);
            case FIELD -> requireOneOf(FIELDS, text);
            case REASON -> requireOneOf(REASONS, text);
            case AUTH_METHOD -> requireOneOf(AUTH_METHODS, text);
            case APPOINTMENT_STATUS -> requireOneOf(APPOINTMENT_STATUSES, text);
            default -> throw invalid("metadata_value_invalid");
        };
    }

    private static String requireOneOf(Set<String> allowed, String value) {
        if (!allowed.contains(value)) {
            throw invalid("metadata_value_invalid");
        }
        return value;
    }

    private static Integer toInteger(Number number) {
        if (number instanceof Integer
                || number instanceof Long
                || number instanceof Short
                || number instanceof Byte) {
            long value = number.longValue();
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return null;
            }
            return (int) value;
        }
        if (number instanceof BigDecimal decimal) {
            try {
                return decimal.intValueExact();
            } catch (ArithmeticException ex) {
                return null;
            }
        }
        return null;
    }

    private static AuditEventValidationException invalid(String code) {
        return new AuditEventValidationException(code);
    }
}
