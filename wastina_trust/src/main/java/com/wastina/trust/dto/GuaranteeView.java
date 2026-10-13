package com.wastina.trust.dto;

import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.GuaranteeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A digital guarantee.
 *
 * <p>Previously the create endpoint returned a fabricated identifier for an entity that was never
 * saved. A guarantee is now persisted, and {@code publicReference} is a real, unique, resolvable
 * identifier. Party identity is exposed as opaque UUIDs only — no email or phone numbers.
 */
@Schema(name = "Guarantee")
public record GuaranteeView(
        @Schema(description = "Stable public reference, e.g. WST-7F3A91C2")
        String publicReference,

        @Schema(description = "Database identifier; treat as opaque")
        Long id,

        @Schema(description = "Guarantee category")
        GuaranteeType type,

        @Schema(description = "Lifecycle state")
        GuaranteeStatus status,

        @Schema(description = "Identifier of the party being guaranteed")
        UUID principalId,

        @Schema(description = "Identifier of the guarantor taking on the liability")
        UUID guarantorId,

        @Schema(description = "Identifier of the beneficiary, when there is one")
        UUID beneficiaryId,

        @Schema(description = "Guaranteed amount")
        BigDecimal amount,

        @Schema(description = "ISO currency code")
        String currency,

        @Schema(description = "Remaining liability still outstanding")
        BigDecimal outstandingLiability,

        @Schema(description = "Short purpose statement")
        String purpose,

        @Schema(description = "Longer description")
        String description,

        @Schema(description = "SHA-256 hash of the canonical contract terms")
        String contractHash,

        @Schema(description = "Version of the terms that were hashed")
        String termsVersion,

        @Schema(description = "Version of the contract that was hashed")
        String contractVersion,

        @Schema(description = "When the guarantee expires")
        Instant expiresAt,

        @Schema(description = "When the guarantee was created")
        LocalDateTime createdAt) {

    public static GuaranteeView from(Guarantee guarantee) {
        return new GuaranteeView(
                guarantee.getPublicReference(),
                guarantee.getId(),
                guarantee.getType(),
                guarantee.getStatus(),
                guarantee.getPrincipal() == null ? null : guarantee.getPrincipal().getId(),
                guarantee.getGuarantor() == null ? null : guarantee.getGuarantor().getId(),
                guarantee.getBeneficiary() == null ? null : guarantee.getBeneficiary().getId(),
                guarantee.getAmount(),
                guarantee.getCurrency(),
                guarantee.getOutstandingLiability(),
                guarantee.getPurpose(),
                guarantee.getDescription(),
                guarantee.getContractHash(),
                guarantee.getTermsVersion(),
                guarantee.getContractVersion(),
                guarantee.getExpiresAt(),
                guarantee.getCreatedAt());
    }
}