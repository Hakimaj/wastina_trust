package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_transaction", columnNames = {"provider", "provider_transaction_id"})
})
@Data
@NoArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id", nullable = false)
    private PaymentIntent paymentIntent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProviderType provider;

    @Column(name = "provider_transaction_id", nullable = false, length = 160)
    private String providerTransactionId;

    @Column(name = "merchant_reference", nullable = false, length = 160)
    private String merchantReference;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 6)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private PaymentTransactionStatus status = PaymentTransactionStatus.CREATED;

    @Column(name = "provider_status", length = 80)
    private String providerStatus;

    @Column(name = "server_verified", nullable = false)
    private boolean serverVerified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Version
    private Long version;

    public void markSuccessful(BigDecimal expectedAmount, String expectedCurrency) {
        if (amount == null || expectedAmount == null || amount.compareTo(expectedAmount) != 0
                || !currency.equalsIgnoreCase(expectedCurrency)) {
            status = PaymentTransactionStatus.RECONCILIATION_REQUIRED;
            throw new IllegalArgumentException("Payment amount or currency does not match the expected intent.");
        }
        status = PaymentTransactionStatus.SUCCESSFUL;
        serverVerified = true;
        verifiedAt = Instant.now();
    }

    public void markUnknown() {
        if (status == PaymentTransactionStatus.SUCCESSFUL || status == PaymentTransactionStatus.REFUNDED) {
            throw new IllegalStateException("A completed transaction cannot become unknown.");
        }
        status = PaymentTransactionStatus.UNKNOWN;
    }
}
