package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhooks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_event", columnNames = {"provider", "provider_event_id"})
})
@Data
@NoArgsConstructor
public class PaymentWebhook {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProviderType provider;

    @Column(name = "provider_event_id", nullable = false, length = 160)
    private String providerEventId;

    @Column(name = "provider_transaction_id", length = 160)
    private String providerTransactionId;

    @Column(name = "payload_digest", nullable = false, length = 128)
    private String payloadDigest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentWebhookStatus status = PaymentWebhookStatus.RECEIVED;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;
}
