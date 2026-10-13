package com.wastina.trust.controller;

/**
 * Signals that a request is well-formed but violates a domain rule, for example an invalid state
 * transition or exceeding guarantor capacity. Mapped to {@code 409 Conflict} with a stable code.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String code;

    public BusinessRuleViolationException(String message) {
        this(ApiErrorCodes.CONFLICT, message);
    }

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}