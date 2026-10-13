package com.wastina.trust.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A refused payment-provider callback.
 *
 * <p>Returns the standard error envelope shape so a provider integration sees the same structure
 * regardless of whether the callback was accepted.
 */
@Schema(name = "RejectedWebhook")
public record RejectedWebhook(
        @Schema(description = "Always 400")
        int status,

        @Schema(description = "Stable error code")
        String code,

        @Schema(description = "Why the callback was refused")
        String message) {

    public static RejectedWebhook reason(String message) {
        return new RejectedWebhook(400, "INVALID_WEBHOOK", message);
    }
}