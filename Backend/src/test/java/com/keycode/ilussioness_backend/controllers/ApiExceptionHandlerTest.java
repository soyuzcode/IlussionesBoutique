package com.keycode.ilussioness_backend.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the global handler maps unexpected failures to short, generic
 * bodies. A 500 is not forced through HTTP because it would require breaking the
 * running data source; this test exercises the same handler method directly.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void unexpectedErrorReturnsGenericInternalErrorWithoutDetails() {
        ResponseEntity<Map<String, String>> response = handler.unexpected(
                new IllegalStateException("database password is secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "internal_error");
        assertThat(response.getBody().toString())
                .doesNotContain("database password is secret")
                .doesNotContain("IllegalStateException")
                .doesNotContain("java.")
                .doesNotContain("Exception");
    }

    @Test
    void dataAccessErrorReturnsServiceUnavailableWithoutDetails() {
        ResponseEntity<Map<String, String>> response = handler.dataAccess(
                new DataAccessResourceFailureException("connection refused at 10.0.0.1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "service_unavailable");
        assertThat(response.getBody().toString())
                .doesNotContain("10.0.0.1")
                .doesNotContain("connection");
    }

    @Test
    void integrityViolationReturnsConflictWithoutDetails() {
        ResponseEntity<Map<String, String>> response = handler.dataConflict(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "data_conflict");
        assertThat(response.getBody().toString())
                .doesNotContain("duplicate key")
                .doesNotContain("constraint");
    }
}
