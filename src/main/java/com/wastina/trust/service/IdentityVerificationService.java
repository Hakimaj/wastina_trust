package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.entity.Consent;
import com.wastina.trust.entity.IdentityStatus;
import com.wastina.trust.entity.IdentityVerification;
import com.wastina.trust.entity.User;
import com.wastina.trust.entity.VerificationStatus;
import com.wastina.trust.repository.ConsentRepository;
import com.wastina.trust.repository.IdentityVerificationRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.security.IdentityVerificationProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identity verification lifecycle.
 *
 * <p>The audit found the flow was a dead end: {@code start()} called the provider and discarded the
 * redirect URL, no endpoint ever called {@code resolve()}, and the only view omitted any redirect.
 * Verification could begin but never finish.
 *
 * <p>Changes:
 * <ul>
 *   <li>{@link StartResult} carries the provider redirect URL through to the caller.</li>
 *   <li>{@link #resolveForUser} completes an attempt that the provider has finished, and is the
 *       secured callback target.</li>
 *   <li>"Already verified" is reported as a specific conflict code instead of a bare
 *       {@code 409}.</li>
 * </ul>
 */
@Service
public class IdentityVerificationService {

    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final IdentityVerificationRepository verificationRepository;
    private final IdentityVerificationProvider provider;

    public IdentityVerificationService(UserRepository userRepository,
                                       ConsentRepository consentRepository,
                                       IdentityVerificationRepository verificationRepository,
                                       IdentityVerificationProvider provider) {
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.verificationRepository = verificationRepository;
        this.provider = provider;
    }

    @Transactional(readOnly = true)
    public Optional<IdentityVerification> findForUser(UUID verificationId, UUID userId) {
        return verificationRepository.findById(verificationId)
                .filter(verification -> verification.getUserId().equals(userId));
    }

    @Transactional(readOnly = true)
    public List<IdentityVerification> listForUser(UUID userId) {
        return verificationRepository.findAllByUserIdOrderByInitiatedAtDesc(userId);
    }

    /**
     * Starts a verification attempt, recording consent first.
     *
     * @return the attempt together with the provider redirect URL the user must visit
     */
    @Transactional
    public StartResult start(UUID userId, String purpose, String policyVersion, String locale) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found: " + userId));
        if (user.getIdentityStatus() == IdentityStatus.VERIFIED) {
            throw new BusinessRuleViolationException(ApiErrorCodes.IDENTITY_ALREADY_VERIFIED,
                    "Identity is already verified.");
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

        return new StartResult(verificationRepository.save(verification), started.redirectUri());
    }

    /**
     * Completes an attempt the caller owns by asking the provider for its result.
     *
     * <p>Repeated calls are safe: an attempt that is no longer {@code PENDING} is returned
     * unchanged rather than being resolved twice.
     */
    @Transactional
    public IdentityVerification resolveForUser(UUID verificationId, UUID userId) {
        IdentityVerification verification = verificationRepository.findById(verificationId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Identity verification was not found: " + verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            return verification;
        }
        return resolve(verification.getProviderReference());
    }

    /**
     * Resolves an attempt from the provider and, on success, marks the user verified.
     *
     * <p>Also reachable from a provider webhook, where the user id is not taken from the request.
     */
    @Transactional
    public IdentityVerification resolve(String providerReference) {
        IdentityVerification verification = verificationRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Identity verification was not found for provider reference " + providerReference));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            return verification;
        }

        IdentityVerificationProvider.VerificationResult result = provider.resolve(providerReference);

        User user = userRepository.findById(verification.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Account was not found: " + verification.getUserId()));

        verification.setStatus(result.successful()
                ? VerificationStatus.SUCCEEDED : VerificationStatus.FAILED);
        verification.setIdentityStatus(result.successful()
                ? IdentityStatus.VERIFIED : IdentityStatus.VERIFICATION_FAILED);
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

    /** A started attempt plus the provider URL the user must visit to continue. */
    public record StartResult(IdentityVerification verification, String redirectUrl) {
    }
}