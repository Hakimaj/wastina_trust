package com.wastina.trust.gateway.payment;

import com.wastina.trust.entity.PaymentIntent;
import com.wastina.trust.entity.PaymentProviderType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test | local | default")
public class MockPaymentProviderGateway implements PaymentProviderGateway {
    private final Map<String, ProviderPayment> payments = new ConcurrentHashMap<>();

    @Override
    public PaymentProviderType provider() {
        return PaymentProviderType.MOCK;
    }

    @Override
    public ProviderPayment create(PaymentIntent intent) {
        String reference = "mock-" + intent.getIdempotencyKey();
        ProviderPayment result = new ProviderPayment(reference, "PENDING", intent.getAmount(), intent.getCurrency(), reference);
        payments.put(reference, result);
        return result;
    }

    @Override
    public ProviderPayment verify(String providerReference) {
        ProviderPayment payment = payments.get(providerReference);
        if (payment == null) {
            throw new IllegalArgumentException("Provider transaction not found");
        }
        return new ProviderPayment(payment.providerReference(), "SUCCESS", payment.amount(), payment.currency(), payment.checkoutReference());
    }

    @Override
    public boolean verifyWebhook(String rawPayload, String signature) {
        return rawPayload != null && signature != null && signature.startsWith("mock-");
    }
}
