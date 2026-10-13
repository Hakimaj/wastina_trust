package com.wastina.trust.security;

import java.util.UUID;

public interface IdentityVerificationProvider {
    VerificationStart start(UUID userId, UUID consentId);

    VerificationResult resolve(String providerReference);

    record VerificationStart(String providerReference, String redirectUri) {}

    record VerificationResult(boolean successful, String assuranceLevel, String failureReasonCode) {}
}