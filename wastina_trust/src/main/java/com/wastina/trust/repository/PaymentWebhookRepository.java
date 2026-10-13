package com.wastina.trust.repository;

import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, UUID> {
    Optional<PaymentWebhook> findByProviderAndProviderEventId(PaymentProviderType provider, String providerEventId);
}
