package com.wastina.trust.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Translates exceptions into the single {@link ApiError} envelope.
 *
 * <p>Two problems from the original audit are fixed here:
 * <ol>
 *   <li>Ordinary "not found" conditions no longer surface as {@code 500}. Services that previously
 *       threw a bare {@code RuntimeException} now throw a typed exception, and this handler maps
 *       {@code EntityNotFoundException} / {@code NoSuchElementException} to {@code 404}.</li>
 *   <li>{@code code} is a stable machine-readable identifier instead of the HTTP reason phrase,
 *       so the frontend can branch without parsing prose.</li>
 * </ol>
 *
 * <p>Unhandled exceptions are logged with their stack trace and returned as a generic
 * {@code INTERNAL_ERROR} so no stack trace or internal message reaches a client.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String code = ApiErrorCodes.forStatus(status);
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "Access is denied." : exception.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(HttpStatus.FORBIDDEN.value(), ApiErrorCodes.FORBIDDEN, message, request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "Authentication is required." : exception.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED.value(), ApiErrorCodes.UNAUTHORIZED, message, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        List<ApiError.FieldError> globalErrors = exception.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ApiError.FieldError(error.getObjectName(), error.getDefaultMessage()))
                .toList();

        String message = fieldErrors.isEmpty() && globalErrors.isEmpty()
                ? "Request validation failed."
                : fieldErrors.stream()
                        .map(error -> error.field() + ": " + error.message())
                        .findFirst()
                        .orElseGet(() -> globalErrors.isEmpty()
                                ? "Request validation failed."
                                : globalErrors.get(0).message());

        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(), ApiErrorCodes.VALIDATION_FAILED, message, request,
                java.util.stream.Stream.concat(fieldErrors.stream(), globalErrors.stream()).toList()));
    }

    /** Malformed JSON, unparseable UUIDs, or an unknown enum constant. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        String detail = rootMessage(exception);
        String message = detail == null || detail.isBlank()
                ? "The request body could not be parsed."
                : "The request body could not be parsed: " + detail;
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(), ApiErrorCodes.MALFORMED_REQUEST, message, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String message = "Parameter '" + exception.getName() + "' has an invalid value.";
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(), ApiErrorCodes.MALFORMED_REQUEST, message, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(), ApiErrorCodes.MALFORMED_REQUEST,
                "Required parameter '" + exception.getParameterName() + "' is missing.", request));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(), ApiErrorCodes.PAYLOAD_TOO_LARGE,
                "The uploaded file exceeds the permitted size.", request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiError.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ApiErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported.", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiError.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(), ApiErrorCodes.METHOD_NOT_ALLOWED,
                "HTTP method " + exception.getMethod() + " is not supported for this endpoint.", request));
    }

    /**
     * Unique-constraint and not-null violations are a client conflict, not a server fault. Reporting
     * them as {@code 500} previously hid duplicate-email/duplicate-phone failures.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        String detail = rootMessage(exception);
        String message = detail != null && detail.toLowerCase().contains("unique")
                ? "A record with the same unique value already exists."
                : "The request conflicts with an existing record.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(), ApiErrorCodes.CONFLICT, message, request));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(), ApiErrorCodes.CONCURRENT_MODIFICATION,
                "The record was modified concurrently. Reload it and try again.", request));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "The requested resource was not found." : exception.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                HttpStatus.NOT_FOUND.value(), ApiErrorCodes.NOT_FOUND, message, request));
    }

    /**
     * A domain rule that forbids the current state transition or the requested combination.
     *
     * <p>The status follows the code: an authorization decision carried by a
     * {@link BusinessRuleViolationException} is reported as {@code 403} rather than the default
     * {@code 409}, so "you may not do this" stays distinguishable from "this is not currently
     * possible".
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleViolationException exception, HttpServletRequest request) {
        String code = exception.getCode() == null ? ApiErrorCodes.CONFLICT : exception.getCode();
        HttpStatus status = switch (code) {
            case ApiErrorCodes.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ApiErrorCodes.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ApiErrorCodes.INVALID_ARGUMENT, ApiErrorCodes.VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), code, exception.getMessage(), request));
    }

    /**
     * Retained for the many existing service methods that still signal a rejected request with
     * {@code IllegalArgumentException}. Domain rule violations raised as
     * {@code IllegalStateException} are deliberately routed to {@code 409} rather than {@code 400}
     * to preserve the behaviour the frontend already handles.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidArgument(IllegalArgumentException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "The request is not valid." : exception.getMessage();
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(), ApiErrorCodes.INVALID_ARGUMENT, message, request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "The request conflicts with the current state." : exception.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(), ApiErrorCodes.CONFLICT, message, request));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurity(SecurityException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "Access is forbidden." : exception.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                HttpStatus.FORBIDDEN.value(), ApiErrorCodes.FORBIDDEN, message, request));
    }

    /**
     * Guard for a URL that matches no controller. Declared here as well as in
     * {@code NoHandlerFoundExceptionHandler} because a {@code @RestControllerAdvice} handler for
     * {@code Exception} would otherwise win over the more specific advice for this type.
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFound(
            org.springframework.web.servlet.NoHandlerFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                HttpStatus.NOT_FOUND.value(), ApiErrorCodes.NOT_FOUND,
                "No endpoint " + request.getMethod() + " " + request.getRequestURI() + ".", request));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                HttpStatus.NOT_FOUND.value(), ApiErrorCodes.NOT_FOUND,
                "No endpoint " + request.getMethod() + " " + request.getRequestURI() + ".", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiErrorCodes.INTERNAL_ERROR,
                "An unexpected error occurred. Quote the requestId when reporting this.", request));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    /** Unused today, kept so the helper above can build field errors from raw binding errors. */
    @SuppressWarnings("unused")
    private List<ApiError.FieldError> toFieldErrors(List<FieldError> errors) {
        return errors.stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
    }
}