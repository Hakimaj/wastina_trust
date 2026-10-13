package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create-guarantee-request body.
 *
 * <p>{@code requesterId} was removed: the requester is always the authenticated caller. The previous
 * DTO let any user file a request attributed to somebody else.
 */
@Schema(name = "GuaranteeRequestCreate")
public record GuaranteeRequestCreate(
        @Schema(description = "Phone number to send the guarantee request to",
                example = "+251911234567", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "is required")
        @Size(min = 7, max = 32, message = "must be between 7 and 32 characters")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$",
                message = "must use digits, spaces, +, ( ) or -")
        String guarantorPhone) {
}