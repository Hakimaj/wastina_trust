package com.wastina.trust.dto;

import com.wastina.trust.entity.PaymentIntent;
import com.wastina.trust.entity.PaymentIntentStatus;
import com.wastina.trust.entity.PaymentProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A payment intent.
 *
 * <p>Replaces the raw entity, which exposed the payer {@code User} (and therefore email and phone
 * number) to any caller who could read an intent.
 */
@Schema(name = "PaymentIntent")
public record PaymentIntentView(
        @Schema(description = "Database identifier; treat as opaque")
        Long id,

        @Schema(description = "Amount to charge")
        BigDecimal amount,

        @Schema(description = "ISO currency code")
        String currency,

        @Schema(description = "Payment provider")
        PaymentProviderType provider,

        @Schema(description = "What the payment is for")
        String purpose,

        @Schema(description = "Client-supplied idempotency key")
        String idempotencyKey,

        @Schema(description = "Provider reference, once the provider has issued one")
        String providerReference,

        @Schema(description = "Intent state")
        PaymentIntentStatus status,

        @Schema(description = "Server-side verification state")
        String verificationStatus,

        @Schema(description = "When the intent was created")
        LocalDateTime createdAt,

        @Schema(description = "When the intent expires")
        Instant expiresAt,

        @Schema(description = "When the intent completed")
        Instant completedAt) {

    public static PaymentIntentView from(PaymentIntent intent) {
        return new PaymentIntentView(
                intent.getId(),
                intent.getAmount(),
                intent.getCurrency(),
                intent.getProvider(),
                intent.getPurpose(),
                intent.getIdempotencyKey(),
                intent.getProviderReference(),
                intent.getStatus(),
                intent.getVerificationStatus(),
                intent.getCreatedAt(),
                intent.getExpiresAt(),
                intent.getCompletedAt());
    }
}