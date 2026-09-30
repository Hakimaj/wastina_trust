package com.wastina.trust.repository;

import com.wastina.trust.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, UUID> {
    Optional<IdentityVerification> findByProviderReference(String providerReference);
}