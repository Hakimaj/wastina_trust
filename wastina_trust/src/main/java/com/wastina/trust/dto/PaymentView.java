package com.wastina.trust.dto;

import com.wastina.trust.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A payment record.
 *
 * <p>Replaces the previous response, which exposed the raw entity shape. The payer is reported as
 * an opaque identifier only.
 */
@Schema(name = "Payment")
public record PaymentView(
        @Schema(description = "Database identifier; treat as opaque")
        Long id,

        @Schema(description = "Provider transaction identifier")
        String transactionId,

        @Schema(description = "Identifier of the payer")
        UUID payerId,

        @Schema(description = "Amount")
        BigDecimal amount,

        @Schema(description = "ISO currency code")
        String currency,

        @Schema(description = "What the payment was for")
        String purpose,

        @Schema(description = "Payment state")
        String status,

        @Schema(description = "When the payment was created")
        LocalDateTime createdAt) {

    public static PaymentView from(Payment payment) {
        return new PaymentView(
                payment.getId(),
                payment.getTransactionId(),
                payment.getPayer() == null ? null : payment.getPayer().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPurpose(),
                payment.getStatus() == null ? null : payment.getStatus().name(),
                payment.getCreatedAt());
    }
}