package com.wastina.trust.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!local & !test")
public class UnavailableIdentityVerificationProvider implements IdentityVerificationProvider {
    @Override
    public VerificationStart start(UUID userId, UUID consentId) {
        throw new IllegalStateException("Fayda provider onboarding and credentials are required");
    }

    @Override
    public VerificationResult resolve(String providerReference) {
        throw new IllegalStateException("Fayda provider onboarding and credentials are required");
    }
}