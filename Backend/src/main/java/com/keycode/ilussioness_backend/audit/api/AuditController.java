package com.keycode.ilussioness_backend.audit.api;

import com.keycode.ilussioness_backend.audit.api.dto.AuditLogDto;
import com.keycode.ilussioness_backend.audit.api.dto.AuditLogSearchRequest;
import com.keycode.ilussioness_backend.audit.domain.AuditLog;
import com.keycode.ilussioness_backend.audit.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestion endpoint for permitted frontend interaction events.
 *
 * <p>The GET endpoints below are read-only and intended for an administrative
 * audit viewer. The application currently has no reliable role-based
 * authorization; access control is therefore not enforced by roles here and
 * must be considered a limitation.</p>
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

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> list(AuditLogSearchRequest request) {
        Page<AuditLogDto> page = auditService.search(request).map(AuditLogDto::from);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(page);
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogDto>> listLogs(AuditLogSearchRequest request) {
        Page<AuditLogDto> page = auditService.search(request).map(AuditLogDto::from);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(page);
    }
}
