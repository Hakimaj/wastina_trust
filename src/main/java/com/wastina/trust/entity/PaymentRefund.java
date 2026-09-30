package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refunds", uniqueConstraints = @UniqueConstraint(name = "uk_payment_refund_idempotency", columnNames = "idempotency_key"))
@Data
@NoArgsConstructor
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransaction paymentTransaction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 6)
    private String currency;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "provider_reference", length = 160)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTransactionStatus status = PaymentTransactionStatus.REFUND_PENDING;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public void validateAgainst(BigDecimal paidAmount, BigDecimal alreadyRefunded) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(paidAmount.subtract(alreadyRefunded)) > 0) {
            throw new IllegalArgumentException("Refund exceeds the remaining refundable amount.");
        }
    }
}
