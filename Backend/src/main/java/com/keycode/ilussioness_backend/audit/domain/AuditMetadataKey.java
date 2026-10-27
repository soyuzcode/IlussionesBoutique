package com.keycode.ilussioness_backend.audit.domain;

/**
 * The complete, bounded set of metadata keys accepted by the audit system.
 *
 * <p>Metadata is deliberately small and non-sensitive. It must never contain
 * passwords, tokens, payment data, free-form user input, DOM text, request
 * bodies or customer contact details.</p>
 */
public enum AuditMetadataKey {
    SECTION,
    OPERATION,
    HTTP_STATUS,
    ERROR_CODE,
    ERROR_NAME,
    FIELD,
    REASON,
    AUTH_METHOD,
    CUSTOMER_ID,
    APPOINTMENT_STATUS,
    PRODUCT_COUNT,
    HAS_DELIVERY_DATE
}
