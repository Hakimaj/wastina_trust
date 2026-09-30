package com.wastina.trust.repository;

import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByProviderAndProviderTransactionId(PaymentProviderType provider, String providerTransactionId);
}
