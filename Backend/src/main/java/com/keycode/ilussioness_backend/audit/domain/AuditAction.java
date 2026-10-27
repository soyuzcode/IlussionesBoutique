package com.keycode.ilussioness_backend.audit.domain;

/**
 * Coarse-grained action associated with an audit event.
 *
 * <p>The action is derived from the event type on the server. Clients never
 * supply it, which prevents a client from claiming a different action than the
 * event type it is allowed to emit.</p>
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    NAVIGATE,
    VIEW,
    OPEN,
    SUBMIT,
    VALIDATION_FAILED,
    REQUEST_FAILED,
    ERROR,
    LOGIN,
    LOGOUT
}
