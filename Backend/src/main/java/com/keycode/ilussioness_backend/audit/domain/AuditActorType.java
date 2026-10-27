package com.keycode.ilussioness_backend.audit.domain;

/**
 * Type of the actor that caused an event.
 *
 * <p>This application does not yet have a trusted authentication mechanism,
 * so all current events are recorded as {@link #ANONYMOUS} with a null actor
 * id. The other values exist for a future authenticated integration.</p>
 */
public enum AuditActorType {
    ANONYMOUS,
    USER,
    SYSTEM
}
