package com.keycode.ilussioness_backend.audit.domain;

import java.util.Set;

/**
 * Catalogue of audit event types.
 *
 * <p>The first group is emitted by the frontend through the ingestion
 * endpoint. The second group is written only by backend business services
 * after a database operation succeeds (or when it fails/rejects). Keeping the
 * two groups distinct prevents a UI interaction from being mistaken for a
 * completed business operation.</p>
 */
public enum AuditEventType {

    // ---------------------------------------------------------------------
    // Frontend interaction events (accepted by POST /api/audit/events)
    // ---------------------------------------------------------------------
    UI_SESSION_STARTED(
            AuditAction.LOGIN,
            Set.of(AuditEntityType.AUTH),
            true,
            Set.of()
    ),
    UI_LOGIN_SUBMITTED(
            AuditAction.LOGIN,
            Set.of(AuditEntityType.AUTH),
            true,
            Set.of(AuditMetadataKey.AUTH_METHOD)
    ),
    UI_LOGOUT_CLICKED(
            AuditAction.LOGOUT,
            Set.of(AuditEntityType.AUTH),
            true,
            Set.of()
    ),
    UI_SECTION_VIEWED(
            AuditAction.NAVIGATE,
            Set.of(AuditEntityType.APPLICATION),
            true,
            Set.of(AuditMetadataKey.SECTION)
    ),
    UI_PRODUCT_CATALOG_VIEWED(
            AuditAction.VIEW,
            Set.of(AuditEntityType.PRODUCT),
            true,
            Set.of(AuditMetadataKey.SECTION)
    ),
    UI_APPOINTMENT_DETAIL_VIEWED(
            AuditAction.VIEW,
            Set.of(AuditEntityType.APPOINTMENT),
            true,
            Set.of()
    ),
    UI_APPOINTMENT_FORM_OPENED(
            AuditAction.OPEN,
            Set.of(AuditEntityType.APPOINTMENT),
            true,
            Set.of()
    ),
    UI_APPOINTMENT_FORM_VALIDATION_FAILED(
            AuditAction.VALIDATION_FAILED,
            Set.of(AuditEntityType.APPOINTMENT),
            true,
            Set.of(AuditMetadataKey.FIELD, AuditMetadataKey.REASON)
    ),
    UI_APPOINTMENT_FORM_SUBMITTED(
            AuditAction.SUBMIT,
            Set.of(AuditEntityType.APPOINTMENT),
            true,
            Set.of()
    ),
    UI_APPOINTMENT_CREATE_FAILED(
            AuditAction.ERROR,
            Set.of(AuditEntityType.APPOINTMENT),
            true,
            Set.of(AuditMetadataKey.REASON)
    ),
    UI_API_REQUEST_FAILED(
            AuditAction.REQUEST_FAILED,
            Set.of(
                    AuditEntityType.CUSTOMER,
                    AuditEntityType.APPOINTMENT,
                    AuditEntityType.ORDER
            ),
            true,
            Set.of(
                    AuditMetadataKey.OPERATION,
                    AuditMetadataKey.HTTP_STATUS,
                    AuditMetadataKey.ERROR_CODE
            )
    ),
    UI_UNEXPECTED_ERROR(
            AuditAction.ERROR,
            Set.of(AuditEntityType.APPLICATION),
            true,
            Set.of(AuditMetadataKey.ERROR_NAME)
    ),

    // ---------------------------------------------------------------------
    // Backend business events (never accepted from the ingestion endpoint)
    // ---------------------------------------------------------------------
    CUSTOMER_CREATED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of()
    ),
    CUSTOMER_CREATE_FAILED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    CUSTOMER_UPDATED(
            AuditAction.UPDATE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of()
    ),
    CUSTOMER_UPDATE_REJECTED(
            AuditAction.UPDATE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    CUSTOMER_UPDATE_FAILED(
            AuditAction.UPDATE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    CUSTOMER_DELETED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of()
    ),
    CUSTOMER_DELETE_REJECTED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    CUSTOMER_DELETE_FAILED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.CUSTOMER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    APPOINTMENT_CREATED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.APPOINTMENT),
            false,
            Set.of(
                    AuditMetadataKey.CUSTOMER_ID,
                    AuditMetadataKey.APPOINTMENT_STATUS
            )
    ),
    APPOINTMENT_CREATE_FAILED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.APPOINTMENT),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    ORDER_CREATED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.ORDER),
            false,
            Set.of(
                    AuditMetadataKey.PRODUCT_COUNT,
                    AuditMetadataKey.HAS_DELIVERY_DATE
            )
    ),
    ORDER_CREATE_FAILED(
            AuditAction.CREATE,
            Set.of(AuditEntityType.ORDER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    ORDER_DELETED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.ORDER),
            false,
            Set.of()
    ),
    ORDER_DELETE_REJECTED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.ORDER),
            false,
            Set.of(AuditMetadataKey.REASON)
    ),
    ORDER_DELETE_FAILED(
            AuditAction.DELETE,
            Set.of(AuditEntityType.ORDER),
            false,
            Set.of(AuditMetadataKey.REASON)
    );

    private final AuditAction action;
    private final Set<AuditEntityType> allowedEntityTypes;
    private final boolean clientIngestible;
    private final Set<AuditMetadataKey> allowedMetadataKeys;

    AuditEventType(
            AuditAction action,
            Set<AuditEntityType> allowedEntityTypes,
            boolean clientIngestible,
            Set<AuditMetadataKey> allowedMetadataKeys
    ) {
        this.action = action;
        this.allowedEntityTypes = Set.copyOf(allowedEntityTypes);
        this.clientIngestible = clientIngestible;
        this.allowedMetadataKeys = Set.copyOf(allowedMetadataKeys);
    }

    public AuditAction getAction() {
        return action;
    }

    public Set<AuditEntityType> getAllowedEntityTypes() {
        return allowedEntityTypes;
    }

    public boolean isClientIngestible() {
        return clientIngestible;
    }

    public Set<AuditMetadataKey> getAllowedMetadataKeys() {
        return allowedMetadataKeys;
    }
}
