package com.wastina.trust.dto;

import com.wastina.trust.entity.GuaranteeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create-guarantee request.
 *
 * <p>The original endpoint accepted an untyped {@code Map<String, Object>} and a
 * {@code principalId} supplied by the caller, which allowed any authenticated user to create a
 * guarantee in someone else's name. The principal is now always the authenticated caller.
 */
@Schema(name = "GuaranteeCreateRequest")
public record GuaranteeCreateRequest(
        @Schema(description = "The guarantor taking on the liability",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        UUID guarantorId,

        @Schema(description = "Optional beneficiary of the guarantee")
        UUID beneficiaryId,

        @Schema(description = "Guaranteed amount", example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @Positive(message = "must be greater than zero")
        @Digits(integer = 16, fraction = 2, message = "must have at most 16 integer and 2 decimal digits")
        @DecimalMax(value = "500000.00", message = "exceeds the per-guarantee limit")
        BigDecimal amount,

        @Schema(description = "ISO currency code", example = "ETB")
        @Size(min = 3, max = 3, message = "must be a 3-letter ISO currency code")
        String currency,

        @Schema(description = "Guarantee category", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        GuaranteeType type,

        @Schema(description = "Short purpose statement", maxLength = 500)
        @Size(max = 500, message = "must be at most 500 characters")
        String purpose,

        @Schema(description = "Longer description", maxLength = 2000)
        @Size(max = 2000, message = "must be at most 2000 characters")
        String description) {
}