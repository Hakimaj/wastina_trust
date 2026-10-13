package com.wastina.trust.security;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic local stand-in for the Fayda identity provider.
 *
 * <p>Registered only under the {@code local} and {@code test} profiles, so it can never be reached in
 * a deployed environment. In every other profile
 * {@link UnavailableIdentityVerificationProvider} is active instead and refuses to start, which is
 * why the audit found identity verification unusable outside local development.
 *
 * <p>Outcomes are deterministic so both paths can be exercised:
 * <ul>
 *   <li>A reference beginning {@code mock-fail-} resolves as a failure.</li>
 *   <li>Any other {@code mock-} reference resolves as a success.</li>
 * </ul>
 */
@Component
@Profile({"local", "test"})
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

    private static final String PREFIX = "mock-";
    private static final String FAILURE_PREFIX = "mock-fail-";

    @Override
    public VerificationStart start(UUID userId, UUID consentId) {
        // Derive the outcome from the user id so a given account always behaves the same way,
        // which keeps tests reproducible.
        boolean shouldFail = Math.abs(userId.hashCode() % 5) == 0;
        String reference = shouldFail
                ? FAILURE_PREFIX + UUID.randomUUID()
                : PREFIX + UUID.randomUUID();
        return new VerificationStart(reference, "/mock-identity/continue?ref=" + reference);
    }

    @Override
    public VerificationResult resolve(String providerReference) {
        if (providerReference == null || !providerReference.startsWith(PREFIX)) {
            return new VerificationResult(false, null, "INVALID_PROVIDER_REFERENCE");
        }
        if (providerReference.startsWith(FAILURE_PREFIX)) {
            return new VerificationResult(false, null, "MOCK_VERIFICATION_FAILED");
        }
        return new VerificationResult(true, "MOCK_TEST_ASSURANCE", null);
    }
}