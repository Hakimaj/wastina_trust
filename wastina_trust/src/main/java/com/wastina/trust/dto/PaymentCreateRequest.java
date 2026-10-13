package com.wastina.trust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create-payment request.
 *
 * <p>{@code payerId} was removed: the payer is always the authenticated caller. The previous DTO
 * let any authenticated user file a payment against another user's account.
 */
@Schema(name = "PaymentCreateRequest")
public record PaymentCreateRequest(
        @Schema(description = "Amount", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        @DecimalMin(value = "0.01", message = "must be greater than zero")
        @Digits(integer = 16, fraction = 2, message = "must have at most 16 integer and 2 decimal digits")
        BigDecimal amount,

        @Schema(description = "ISO currency code", example = "ETB")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO currency code")
        String currency,

        @Schema(description = "What the payment is for", maxLength = 120)
        @Size(max = 120, message = "must be at most 120 characters")
        String purpose) {
}