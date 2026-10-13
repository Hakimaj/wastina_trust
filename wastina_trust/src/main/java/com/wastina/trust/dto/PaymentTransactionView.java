package com.wastina.trust.dto;

import com.wastina.trust.entity.PaymentTransaction;
import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentTransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A provider payment transaction.
 *
 * <p>Replaces the raw entity, which exposed the nested payment intent.
 */
@Schema(name = "PaymentTransaction")
public record PaymentTransactionView(
        @Schema(description = "Transaction identifier")
        UUID id,

        @Schema(description = "Identifier of the payment intent this belongs to")
        Long paymentIntentId,

        @Schema(description = "Payment provider")
        PaymentProviderType provider,

        @Schema(description = "Provider-side transaction identifier")
        String providerTransactionId,

        @Schema(description = "Reference echoed back to the provider")
        String merchantReference,

        @Schema(description = "Amount")
        BigDecimal amount,

        @Schema(description = "ISO currency code")
        String currency,

        @Schema(description = "Transaction state")
        PaymentTransactionStatus status,

        @Schema(description = "Status reported by the provider")
        String providerStatus,

        @Schema(description = "Whether the server has verified this transaction")
        boolean serverVerified,

        @Schema(description = "When the server verified the transaction")
        Instant verifiedAt) {

    public static PaymentTransactionView from(PaymentTransaction transaction) {
        return new PaymentTransactionView(
                transaction.getId(),
                transaction.getPaymentIntent() == null ? null : transaction.getPaymentIntent().getId(),
                transaction.getProvider(),
                transaction.getProviderTransactionId(),
                transaction.getMerchantReference(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getProviderStatus(),
                transaction.isServerVerified(),
                transaction.getVerifiedAt());
    }
}