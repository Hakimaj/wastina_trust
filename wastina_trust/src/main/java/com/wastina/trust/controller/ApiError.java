package com.wastina.trust.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The single error envelope returned by every API endpoint.
 *
 * <p>Kept backward compatible with the original contract: {@code status}, {@code code} and
 * {@code message} are always present and keep their meaning. {@code code} is now a stable,
 * machine-readable identifier (for example {@code VALIDATION_FAILED}) rather than the HTTP reason
 * phrase, so a client can branch on it without parsing prose.
 */
public record ApiError(
        int status,
        String code,
        String message,
        String path,
        String requestId,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    /** Minimal factory for errors that carry no field-level detail. */
    public static ApiError of(int status, String code, String message, HttpServletRequest request) {
        return new ApiError(status, code, message,
                request == null ? null : request.getRequestURI(),
                com.wastina.trust.web.RequestIdFilter.currentRequestId(request),
                Instant.now(), List.of());
    }

    public static ApiError of(int status, String code, String message, HttpServletRequest request,
                              List<FieldError> fieldErrors) {
        return new ApiError(status, code, message,
                request == null ? null : request.getRequestURI(),
                com.wastina.trust.web.RequestIdFilter.currentRequestId(request),
                Instant.now(), fieldErrors == null ? List.of() : fieldErrors);
    }

    public static List<FieldError> fieldErrors(org.springframework.validation.FieldError... errors) {
        return java.util.Arrays.stream(errors)
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
    }
}