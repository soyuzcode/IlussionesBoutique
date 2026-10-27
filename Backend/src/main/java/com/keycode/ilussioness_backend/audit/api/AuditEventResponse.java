package com.keycode.ilussioness_backend.audit.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Minimal acknowledgement for a recorded frontend event. */
public record AuditEventResponse(UUID eventId, OffsetDateTime occurredAt) {
}
