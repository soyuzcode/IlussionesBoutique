package com.keycode.ilussioness_backend.audit.api;

import com.keycode.ilussioness_backend.audit.service.AuditEventValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Scoped error handling for audit ingestion.
 *
 * <p>Responses never echo the rejected payload or a persistence exception
 * message. Persistence exceptions are logged by class name only so SQL
 * parameters/metadata are not copied into application logs.</p>
 */
@RestControllerAdvice(assignableTypes = AuditController.class)
public class AuditExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuditExceptionHandler.class);

    @ExceptionHandler(AuditEventValidationException.class)
    ResponseEntity<Map<String, String>> invalidAuditEvent(AuditEventValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_audit_event",
                "reason", ex.getCode()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidRequestBody(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_audit_event",
                "reason", "validation_failed"
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> malformedRequestBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_audit_event",
                "reason", "malformed_payload"
        ));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> auditUnavailable(DataAccessException ex) {
        LOG.warn("Audit persistence failed ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "audit_unavailable"
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpectedFailure(Exception ex) {
        LOG.error("Unexpected audit ingestion failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "audit_unavailable"
        ));
    }
}
