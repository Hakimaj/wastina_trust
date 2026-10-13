package com.wastina.trust.dto;

import com.wastina.trust.entity.DigitalContract;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A digital contract, reported by identifier only. No party email addresses or phone numbers.
 */
@Schema(name = "DigitalContract")
public record DigitalContractResponseDto(
        @Schema(description = "Database identifier; treat as opaque")
        Long id,

        @Schema(description = "Worker party identifier")
        UUID workerId,

        @Schema(description = "Guarantor party identifier")
        UUID guarantorId,

        @Schema(description = "Employer party identifier, when present")
        UUID employerId,

        @Schema(description = "Liability amount")
        BigDecimal liabilityAmount,

        @Schema(description = "ISO currency code")
        String currency,

        @Schema(description = "Server-computed SHA-256 hash of the contract terms")
        String termsHash,

        @Schema(description = "When the contract was signed")
        LocalDateTime signedDate) {

    public static DigitalContractResponseDto from(DigitalContract contract) {
        return new DigitalContractResponseDto(
                contract.getId(),
                contract.getWorker() == null ? null : contract.getWorker().getId(),
                contract.getGuarantor() == null ? null : contract.getGuarantor().getId(),
                contract.getEmployer() == null ? null : contract.getEmployer().getId(),
                contract.getLiabilityAmount(),
                "ETB",
                contract.getTermsHash(),
                contract.getSignedDate());
    }
}