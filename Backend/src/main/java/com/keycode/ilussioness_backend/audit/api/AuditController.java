package com.keycode.ilussioness_backend.audit.api;

import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestion endpoint for permitted frontend interaction events.
 *
 * <p>There is deliberately no GET/update/delete endpoint for audit records.
 * The endpoint is public because the rest of the application is currently
 * public; it is protected by validation, a body-size limit and a best-effort
 * per-instance rate limit.</p>
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/events")
    public ResponseEntity<AuditEventResponse> ingest(
            @Valid @RequestBody AuditEventRequest request
    ) {
        AuditLog event = auditService.recordClientEvent(request);
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new AuditEventResponse(event.getId(), event.getOccurredAt()));
    }
}
