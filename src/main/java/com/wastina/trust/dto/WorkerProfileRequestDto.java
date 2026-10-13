package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Upsert request for a worker profile.
 *
 * <p>Field names and semantics are unchanged from the original DTO, so the existing frontend form
 * keeps working; only server-side validation is added.
 */
@Schema(name = "WorkerProfileRequest", description = "Create or update a worker profile")
public record WorkerProfileRequestDto(
        @Schema(description = "Primary profession name", example = "Electrician")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String professionName,

        @Schema(description = "Primary work location name", example = "Addis Ababa")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String locationName,

        @Schema(description = "Whether the worker is currently open to work")
        boolean isAvailable) {
}