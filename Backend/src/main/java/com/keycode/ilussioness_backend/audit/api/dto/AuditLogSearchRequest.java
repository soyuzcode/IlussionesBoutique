package com.keycode.ilussioness_backend.audit.api.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record AuditLogSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        String eventType,
        String entityType,
        String eventSource,
        String outcome,
        String actorType,
        String actorId,
        String entityId,
        String requestId,
        String sessionId,
        String q,
        Integer page,
        Integer size
) {
    public int pageOrDefault() {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public int sizeOrDefault() {
        if (size == null || size < 1) {
            return 50;
        }
        if (size > 200) {
            return 200;
        }
        return size;
    }
}
