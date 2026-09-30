package com.wastina.trust.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"local", "test"})
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {
    @Override
    public VerificationStart start(UUID userId, UUID consentId) {
        return new VerificationStart("mock-" + UUID.randomUUID(), "/mock-identity/continue");
    }

    @Override
    public VerificationResult resolve(String providerReference) {
        if (providerReference == null || !providerReference.startsWith("mock-")) {
            return new VerificationResult(false, null, "INVALID_PROVIDER_REFERENCE");
        }
        return new VerificationResult(true, "MOCK_TEST_ASSURANCE", null);
    }
}