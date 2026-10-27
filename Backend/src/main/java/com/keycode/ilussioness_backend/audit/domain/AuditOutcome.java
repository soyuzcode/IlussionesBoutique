package com.keycode.ilussioness_backend.audit.domain;

/**
 * Outcome of an event.
 *
 * <p>Frontend interaction events are always stored as {@link #INFO}: the
 * browser cannot authoritatively assert that a business operation succeeded.
 * Only backend business events use {@link #SUCCESS}, {@link #FAILURE} or
 * {@link #REJECTED}.</p>
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE,
    REJECTED,
    INFO
}
