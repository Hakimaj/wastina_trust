package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import com.wastina.trust.gateway.payment.PaymentProviderGateway;
import com.wastina.trust.repository.PaymentIntentRepository;
import com.wastina.trust.repository.PaymentTransactionRepository;
import com.wastina.trust.repository.PaymentWebhookRepository;
import com.wastina.trust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {
    private final PaymentIntentRepository intentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentWebhookRepository webhookRepository;
    private final UserRepository userRepository;
    private final List<PaymentProviderGateway> providers;

    @Transactional
    public PaymentIntent createIntent(UUID payerId, BigDecimal amount, String currency,
                                      PaymentProviderType providerType, String purpose, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0 || currency == null || currency.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Amount, currency, and idempotency key are required.");
        }
        PaymentIntent existing = intentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.getAmount().compareTo(amount) != 0 || !existing.getCurrency().equalsIgnoreCase(currency)) {
                throw new IllegalArgumentException("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST");
            }
            return existing;
        }
        PaymentIntent intent = new PaymentIntent();
        intent.setPayer(userRepository.findById(payerId).orElseThrow(() -> new IllegalArgumentException("Payer not found")));
        intent.setAmount(amount);
        intent.setCurrency(currency.toUpperCase());
        intent.setProvider(providerType);
        intent.setPurpose(purpose);
        intent.setIdempotencyKey(idempotencyKey);
        intent.setExpiresAt(Instant.now().plusSeconds(900));
        return intentRepository.save(intent);
    }

    /**
     * Initializes a transaction, requiring the caller to be the payer of the intent.
     *
     * <p>Previously this accepted only an intent id, so any authenticated user could drive another
     * user's payment forward.
     */
    @Transactional
    public PaymentTransaction initializeTransactionForPayer(Long intentId, UUID payerId) {
        PaymentIntent intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new com.wastina.trust.controller.EntityNotFoundException(
                        "Payment intent was not found: " + intentId));

        if (intent.getPayer() == null || !intent.getPayer().getId().equals(payerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not the payer of this payment intent.");
        }
        return initializeTransaction(intent);
    }

    @Transactional
    public PaymentTransaction initializeTransaction(Long intentId) {
        PaymentIntent intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found"));
        return initializeTransaction(intent);
    }

    private PaymentTransaction initializeTransaction(PaymentIntent intent) {
        PaymentProviderGateway provider = providers.stream().filter(candidate -> candidate.provider() == intent.getProvider())
                .findFirst().orElseThrow(() -> new IllegalStateException("Payment provider is not configured"));
        PaymentProviderGateway.ProviderPayment providerPayment = provider.create(intent);
        intent.setProviderReference(providerPayment.providerReference());
        intent.setStatus(PaymentIntentStatus.PROCESSING);
        intentRepository.save(intent);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentIntent(intent);
        transaction.setProvider(intent.getProvider());
        transaction.setProviderTransactionId(providerPayment.providerReference());
        transaction.setMerchantReference(providerPayment.checkoutReference());
        transaction.setAmount(providerPayment.amount());
        transaction.setCurrency(providerPayment.currency());
        transaction.setStatus(PaymentTransactionStatus.PROCESSING);
        transaction.setProviderStatus(providerPayment.providerStatus());
        return transactionRepository.save(transaction);
    }

    /** Verifies a transaction, requiring the caller to be the payer of its intent. */
    @Transactional
    public PaymentTransaction verifyTransactionForPayer(UUID transactionId, UUID payerId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new com.wastina.trust.controller.EntityNotFoundException(
                        "Payment transaction was not found: " + transactionId));

        PaymentIntent intent = transaction.getPaymentIntent();
        if (intent == null || intent.getPayer() == null || !intent.getPayer().getId().equals(payerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not the payer of this payment transaction.");
        }
        return verifyTransaction(transaction);
    }

    @Transactional
    public PaymentTransaction verifyTransaction(UUID transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));
        return verifyTransaction(transaction);
    }

    private PaymentTransaction verifyTransaction(PaymentTransaction transaction) {
        PaymentProviderGateway provider = providers.stream().filter(candidate -> candidate.provider() == transaction.getProvider())
                .findFirst().orElseThrow(() -> new IllegalStateException("Payment provider is not configured"));
        PaymentProviderGateway.ProviderPayment providerPayment = provider.verify(transaction.getProviderTransactionId());
        transaction.setProviderStatus(providerPayment.providerStatus());
        transaction.markSuccessful(transaction.getPaymentIntent().getAmount(), transaction.getPaymentIntent().getCurrency());
        transaction.getPaymentIntent().setStatus(PaymentIntentStatus.SUCCESS);
        transaction.getPaymentIntent().setVerificationStatus("VERIFIED");
        transaction.getPaymentIntent().setCompletedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public PaymentWebhook receiveWebhook(PaymentProviderType providerType, String providerEventId,
                                         String providerTransactionId, String rawPayload, String signature) {
        PaymentProviderGateway provider = providers.stream().filter(candidate -> candidate.provider() == providerType)
                .findFirst().orElseThrow(() -> new IllegalStateException("Payment provider is not configured"));
        PaymentWebhook existing = webhookRepository.findByProviderAndProviderEventId(providerType, providerEventId).orElse(null);
        if (existing != null) {
            existing.setStatus(PaymentWebhookStatus.DUPLICATE);
            return existing;
        }
        PaymentWebhook webhook = new PaymentWebhook();
        webhook.setProvider(providerType);
        webhook.setProviderEventId(providerEventId);
        webhook.setProviderTransactionId(providerTransactionId);
        webhook.setPayloadDigest(sha256(rawPayload));
        webhook.setSignatureValid(provider.verifyWebhook(rawPayload, signature));
        webhook.setStatus(webhook.isSignatureValid() ? PaymentWebhookStatus.PENDING : PaymentWebhookStatus.SIGNATURE_REJECTED);
        return webhookRepository.save(webhook);
    }

    private String sha256(String payload) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
