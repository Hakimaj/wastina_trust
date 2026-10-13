package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create-contract request.
 *
 * <p>{@code workerId} and {@code termsHash} were removed. The worker is always the authenticated
 * caller, and the terms hash is computed server-side so a client cannot assert a hash of its own
 * choosing.
 */
@Schema(name = "DigitalContractRequest")
public record DigitalContractRequestDto(
        @Schema(description = "The guarantor party", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        UUID guarantorId,

        @Schema(description = "Optional employer party")
        UUID employerId,

        @Schema(description = "Liability amount", example = "50000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @Positive(message = "must be greater than zero")
        @Digits(integer = 16, fraction = 2, message = "must have at most 16 integer and 2 decimal digits")
        @DecimalMax(value = "500000.00", message = "exceeds the per-contract liability limit")
        BigDecimal liabilityAmount) {
}