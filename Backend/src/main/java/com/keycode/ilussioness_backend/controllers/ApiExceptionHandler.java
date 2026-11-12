package com.keycode.ilussioness_backend.controllers;

import com.keycode.ilussioness_backend.services.BusinessValidationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error handling for the business REST controllers.
 *
 * <p>Responses never echo the rejected payload, exception messages, SQL
 * statements or stack traces. Persistence failures are logged by class name
 * only so column/constraint details are not copied into client responses.</p>
 */
@RestControllerAdvice(assignableTypes = {
        CustomerController.class,
        AppointmentController.class,
        OrderController.class
})
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Bean Validation failure on a request body ({@code @Valid}). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed",
                "fields", fields
        ));
    }

    /** Bean Validation failure on request parameters / path variables. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<Map<String, String>> invalidParameters(HandlerMethodValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed"
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, String>> invalidConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed"
        ));
    }

    /** Service-level business rule violation. */
    @ExceptionHandler(BusinessValidationException.class)
    ResponseEntity<Map<String, String>> businessRule(BusinessValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed",
                "reason", ex.getCode()
        ));
    }

    /** A path variable that cannot be converted (e.g. a malformed UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> invalidPathVariable(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "reason", "malformed_parameter"
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> malformedBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "reason", "malformed_payload"
        ));
    }

    /** Database constraint conflict (duplicate key, foreign key, check). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> dataConflict(DataIntegrityViolationException ex) {
        LOG.warn("Business persistence constraint violation ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "data_conflict"
        ));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> dataAccess(DataAccessException ex) {
        LOG.error("Business persistence failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "service_unavailable"
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        LOG.error("Unexpected business request failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error"
        ));
    }
}
