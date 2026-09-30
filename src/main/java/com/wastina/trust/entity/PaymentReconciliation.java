package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_reconciliations")
@Data
@NoArgsConstructor
public class PaymentReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransaction paymentTransaction;

    @Column(name = "expected_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "provider_amount", precision = 18, scale = 2)
    private BigDecimal providerAmount;

    @Column(name = "expected_currency", nullable = false, length = 6)
    private String expectedCurrency;

    @Column(name = "provider_currency", length = 6)
    private String providerCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationStatus status = ReconciliationStatus.OPEN;

    @Column(length = 1000)
    private String discrepancy;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "resolved_by")
    private UUID resolvedBy;
}
