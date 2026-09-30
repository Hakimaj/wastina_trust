package com.wastina.trust.service.provider;

import com.wastina.trust.entity.PaymentIntent;
import com.wastina.trust.entity.PaymentIntentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MockPaymentProvider implements PaymentProvider {
    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntent paymentIntent) {
        paymentIntent.setProviderReference("mock-ref-" + System.currentTimeMillis());
        paymentIntent.setStatus(PaymentIntentStatus.PENDING_PROVIDER);
        return paymentIntent;
    }

    @Override
    public PaymentIntent verifyPayment(PaymentIntent paymentIntent) {
        if (paymentIntent.getStatus() == PaymentIntentStatus.PENDING_PROVIDER || paymentIntent.getStatus() == PaymentIntentStatus.PROCESSING) {
            paymentIntent.setStatus(PaymentIntentStatus.SUCCESS);
            paymentIntent.setCompletedAt(Instant.now());
            paymentIntent.setVerificationStatus("VERIFIED");
        }
        return paymentIntent;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        return payload != null && signature != null && secret != null && payload.contains("provider") && signature.startsWith("mock-");
    }
}
