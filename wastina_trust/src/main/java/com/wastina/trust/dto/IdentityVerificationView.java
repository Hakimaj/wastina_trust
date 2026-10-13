package com.wastina.trust.dto;

import com.wastina.trust.entity.IdentityVerification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * An identity verification attempt.
 *
 * <p>{@code redirectUrl} is the previously missing piece: the audit found the provider's redirect
 * URL was computed and then discarded, so a user could start verification but had no way to
 * complete it and no screen to send them to. When verification needs an out-of-band step this is
 * where the user is sent; it is {@code null} once the attempt has finished.
 */
@Schema(name = "IdentityVerification")
public record IdentityVerificationView(
        @Schema(description = "Verification attempt identifier")
        UUID id,

        @Schema(description = "Provider that performs the check")
        String provider,

        @Schema(description = "Provider-side reference for this attempt")
        String providerReference,

        @Schema(description = "Attempt state")
        String status,

        @Schema(description = "Resulting identity status")
        String identityStatus,

        @Schema(description = "Assurance level achieved, when successful")
        String assuranceLevel,

        @Schema(description = "Machine-readable failure reason, when failed")
        String failureReasonCode,

        @Schema(description = "Where the user must be sent to complete verification, when required")
        String redirectUrl,

        @Schema(description = "True while the user still has to complete an out-of-band step")
        boolean actionRequired,

        @Schema(description = "Consent recorded for this attempt")
        UUID consentId,

        @Schema(description = "When the attempt started")
        Instant initiatedAt,

        @Schema(description = "When the attempt finished, if it has")
        Instant completedAt) {

    public static IdentityVerificationView from(IdentityVerification verification, String redirectUrl) {
        boolean outstanding = verification.getStatus() != null
                && verification.getStatus() == com.wastina.trust.entity.VerificationStatus.PENDING;
        return new IdentityVerificationView(
                verification.getId(),
                verification.getProvider(),
                verification.getProviderReference(),
                verification.getStatus() == null ? null : verification.getStatus().name(),
                verification.getIdentityStatus() == null ? null : verification.getIdentityStatus().name(),
                verification.getAssuranceLevel(),
                verification.getFailureReasonCode(),
                redirectUrl,
                outstanding && redirectUrl != null && !redirectUrl.isBlank(),
                verification.getConsentId(),
                verification.getInitiatedAt(),
                verification.getCompletedAt());
    }
}