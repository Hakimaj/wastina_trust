package com.wastina.trust.gateway.payment;

import com.wastina.trust.entity.PaymentIntent;
import com.wastina.trust.entity.PaymentProviderType;

import java.math.BigDecimal;

public interface PaymentProviderGateway {
    PaymentProviderType provider();

    ProviderPayment create(PaymentIntent intent);

    ProviderPayment verify(String providerReference);

    boolean verifyWebhook(String rawPayload, String signature);

    record ProviderPayment(String providerReference, String providerStatus, BigDecimal amount,
                           String currency, String checkoutReference) {}
}
