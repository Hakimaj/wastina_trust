package com.wastina.trust.service.provider;

import com.wastina.trust.entity.PaymentIntent;

public interface PaymentProvider {
    String providerName();

    PaymentIntent createPaymentIntent(PaymentIntent paymentIntent);

    PaymentIntent verifyPayment(PaymentIntent paymentIntent);

    boolean verifyWebhookSignature(String payload, String signature, String secret);
}
