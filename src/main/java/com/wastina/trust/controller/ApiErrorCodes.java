package com.wastina.trust.controller;

/**
 * Stable, machine-readable error codes returned in {@link ApiError#code()}.
 *
 * <p>These are part of the public API contract: a client may branch on them, so they must not be
 * renamed casually. They intentionally describe <em>what kind of problem occurred</em> rather than
 * the specific resource, because the detail belongs in {@code message}.
 */
public final class ApiErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Domain-specific codes.
    public static final String IDENTITY_ALREADY_VERIFIED = "IDENTITY_ALREADY_VERIFIED";
    public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
    public static final String INSUFFICIENT_GUARANTEE_CAPACITY = "INSUFFICIENT_GUARANTEE_CAPACITY";
    public static final String ORGANIZATION_MEMBERSHIP_REQUIRED = "ORGANIZATION_MEMBERSHIP_REQUIRED";
    public static final String ORGANIZATION_NAME_TAKEN = "ORGANIZATION_NAME_TAKEN";
    public static final String MEMBERSHIP_ALREADY_EXISTS = "MEMBERSHIP_ALREADY_EXISTS";
    public static final String ORGANIZATION_NOT_JOINABLE = "ORGANIZATION_NOT_JOINABLE";

    private ApiErrorCodes() {
    }

    /** Maps an HTTP status onto a generic code when no more specific one applies. */
    public static String forStatus(org.springframework.http.HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> INVALID_ARGUMENT;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> CONFLICT;
            case METHOD_NOT_ALLOWED -> METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            case PAYLOAD_TOO_LARGE -> PAYLOAD_TOO_LARGE;
            default -> status.is5xxServerError() ? INTERNAL_ERROR : INVALID_ARGUMENT;
        };
    }
}