package com.keycode.ilussioness_backend.audit.service;

/**
 * Validation failure for an audit payload. The message/code is safe to return
 * to a client and never contains the rejected value.
 */
public class AuditEventValidationException extends RuntimeException {

    private final String code;

    public AuditEventValidationException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
