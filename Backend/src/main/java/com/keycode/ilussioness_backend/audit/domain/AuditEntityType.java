package com.keycode.ilussioness_backend.audit.domain;

/** Business entity affected by an audit event. */
public enum AuditEntityType {
    APPLICATION,
    AUTH,
    CUSTOMER,
    APPOINTMENT,
    ORDER,
    PRODUCT
}
