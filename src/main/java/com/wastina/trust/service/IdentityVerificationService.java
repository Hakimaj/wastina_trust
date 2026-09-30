package com.wastina.trust.service;

import com.wastina.trust.entity.Consent;
import com.wastina.trust.entity.IdentityStatus;
import com.wastina.trust.entity.IdentityVerification;
import com.wastina.trust.entity.User;
import com.wastina.trust.entity.VerificationStatus;
import com.wastina.trust.repository.ConsentRepository;
import com.wastina.trust.repository.IdentityVerificationRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.security.IdentityVerificationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final IdentityVerificationRepository verificationRepository;
    private final IdentityVerificationProvider provider;

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Optional<IdentityVerification> findForUser(UUID verificationId, UUID userId) {
        return verificationRepository.findById(verificationId)
                .filter(verification -> verification.getUserId().equals(userId));
    }

    @Transactional
    @SuppressWarnings("null")
    public IdentityVerification start(UUID userId, String purpose, String policyVersion, String locale) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("IAM user not found"));
        if (user.getIdentityStatus() == IdentityStatus.VERIFIED) {
            throw new IllegalStateException("Identity is already verified");
        }

        Consent consent = new Consent();
        consent.setUserId(userId);
        consent.setPurpose(purpose);
        consent.setPolicyVersion(policyVersion);
        consent.setGrantedAt(Instant.now());
        consent.setSource("WASTINA_IAM");
        consent.setLocale(locale);
        consent = consentRepository.save(consent);

        IdentityVerificationProvider.VerificationStart started = provider.start(userId, consent.getId());
        IdentityVerification verification = new IdentityVerification();
        verification.setUserId(userId);
        verification.setProvider(provider.getClass().getSimpleName());
        verification.setProviderReference(started.providerReference());
        verification.setStatus(VerificationStatus.PENDING);
        verification.setIdentityStatus(IdentityStatus.VERIFICATION_PENDING);
        verification.setConsentId(consent.getId());
        verification.setInitiatedAt(Instant.now());
        user.setIdentityStatus(IdentityStatus.VERIFICATION_PENDING);
        userRepository.save(user);
        return verificationRepository.save(verification);
    }

    @Transactional
    @SuppressWarnings("null")
    public IdentityVerification resolve(String providerReference) {
        IdentityVerification verification = verificationRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        if (verification.getStatus() != VerificationStatus.PENDING) {
            return verification;
        }
        IdentityVerificationProvider.VerificationResult result = provider.resolve(providerReference);
        User user = userRepository.findById(verification.getUserId())
                .orElseThrow(() -> new IllegalStateException("IAM user not found"));
        verification.setStatus(result.successful() ? VerificationStatus.SUCCEEDED : VerificationStatus.FAILED);
        verification.setIdentityStatus(result.successful() ? IdentityStatus.VERIFIED : IdentityStatus.VERIFICATION_FAILED);
        verification.setAssuranceLevel(result.assuranceLevel());
        verification.setFailureReasonCode(result.failureReasonCode());
        verification.setCompletedAt(Instant.now());
        user.setIdentityStatus(verification.getIdentityStatus());
        if (result.successful()) {
            user.setVerifiedAt(Instant.now());
        }
        userRepository.save(user);
        return verificationRepository.save(verification);
    }
}