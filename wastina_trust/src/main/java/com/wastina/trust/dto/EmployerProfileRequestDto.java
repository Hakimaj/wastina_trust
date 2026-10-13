package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Upsert request for an employer profile.
 *
 * <p>{@code subscriptionTier} is now constrained to the values the billing tier actually supports,
 * which the previous free-text field accepted without checking.
 */
@Schema(name = "EmployerProfileRequest", description = "Create or update an employer profile")
public record EmployerProfileRequestDto(
        @Schema(description = "Registered company name", example = "Wastina Construction PLC")
        @NotNull(message = "is required")
        @Size(min = 2, max = 200, message = "must be between 2 and 200 characters")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String companyName,

        @Schema(description = "Company location name", example = "Addis Ababa")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters")
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String locationName,

        @Schema(description = "Subscription tier", example = "FREE", allowableValues = {"FREE", "PREMIUM"})
        @NotNull(message = "is required")
        @Pattern(regexp = "(?i)FREE|PREMIUM", message = "must be either FREE or PREMIUM")
        String subscriptionTier) {
}