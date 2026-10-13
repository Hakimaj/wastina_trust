package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Upsert request for a guarantor profile.
 *
 * <p>{@code kycStatus} was removed: KYC is a trust signal and letting the profile owner set it
 * directly let anybody self-declare themselves verified. It is now maintained by the identity
 * verification flow and is read-only in {@link GuarantorProfileResponseDto}.
 */
@Schema(name = "GuarantorProfileRequest", description = "Create or update a guarantor profile")
public record GuarantorProfileRequestDto(
        @Schema(description = "Maximum total liability this guarantor is willing to back",
                example = "250000.00")
        @PositiveOrZero(message = "must not be negative")
        @Digits(integer = 16, fraction = 2, message = "must have at most 16 integer and 2 decimal digits")
        BigDecimal financialLimit,

        @Schema(description = "Employer the guarantor works for", example = "Addis Ababa City Administration")
        @NotNull(message = "is required")
        @Size(min = 2, max = 200, message = "must be between 2 and 200 characters")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String employerName) {
}