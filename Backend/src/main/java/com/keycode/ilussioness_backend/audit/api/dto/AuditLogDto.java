package com.keycode.ilussioness_backend.audit.api.dto;

import com.keycode.ilussioness_backend.audit.domain.AuditAction;
import com.keycode.ilussioness_backend.audit.domain.AuditActorType;
import com.keycode.ilussioness_backend.audit.domain.AuditEntityType;
import com.keycode.ilussioness_backend.audit.domain.AuditEventType;
import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.domain.AuditOutcome;
import com.keycode.ilussioness_backend.audit.domain.AuditSource;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        OffsetDateTime occurredAt,
        AuditEventType eventType,
        AuditAction action,
        AuditActorType actorType,
        String actorId,
        AuditEntityType entityType,
        String entityId,
        AuditSource eventSource,
        AuditOutcome outcome,
        String sessionId,
        String requestId,
        Map<String, Object> metadata
) {
    public static AuditLogDto from(AuditLog auditLog) {
        Map<String, Object> meta = auditLog.getMetadata();
        if (meta == null) {
            meta = new LinkedHashMap<>();
        } else {
            meta = new LinkedHashMap<>(meta);
        }
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getOccurredAt(),
                auditLog.getEventType(),
                auditLog.getAction(),
                auditLog.getActorType(),
                auditLog.getActorId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getEventSource(),
                auditLog.getOutcome(),
                auditLog.getSessionId(),
                auditLog.getRequestId(),
                meta
        );
    }
}
