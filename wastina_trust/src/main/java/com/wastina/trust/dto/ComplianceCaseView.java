package com.wastina.trust.dto;

import com.wastina.trust.entity.ComplianceCase;
import com.wastina.trust.entity.DecisionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** A compliance case. Parties are opaque identifiers; no nested user data. */
@Schema(name = "ComplianceCase")
public record ComplianceCaseView(
        @Schema(description = "Public reference, e.g. WST-CASE-1A2B3C4D")
        String publicReference,

        @Schema(description = "Database identifier; treat as opaque")
        UUID id,

        @Schema(description = "Complaint this case originated from, when any")
        UUID complaintId,

        @Schema(description = "Identifier of the subject of the case")
        UUID subjectId,

        @Schema(description = "Lifecycle state")
        String status,

        @Schema(description = "Assigned severity, when set")
        String severity,

        @Schema(description = "Recorded decision, when decided")
        DecisionType decisionType,

        @Schema(description = "Reason accompanying the decision")
        String decisionReason,

        @Schema(description = "Policy version applied")
        String policyVersion,

        @Schema(description = "When the case was opened")
        Instant openedAt,

        @Schema(description = "When the case was decided, if it has been")
        Instant decidedAt,

        @Schema(description = "When the case was finalized, if it has been")
        Instant finalizedAt) {

    public static ComplianceCaseView from(ComplianceCase complianceCase) {
        return new ComplianceCaseView(
                complianceCase.getPublicReference(),
                complianceCase.getId(),
                complianceCase.getComplaint() == null ? null : complianceCase.getComplaint().getId(),
                complianceCase.getSubject() == null ? null : complianceCase.getSubject().getId(),
                complianceCase.getStatus() == null ? null : complianceCase.getStatus().name(),
                complianceCase.getSeverity() == null ? null : complianceCase.getSeverity().name(),
                complianceCase.getDecisionType(),
                complianceCase.getDecisionReason(),
                complianceCase.getPolicyVersion(),
                complianceCase.getOpenedAt(),
                complianceCase.getDecidedAt(),
                complianceCase.getFinalizedAt());
    }
}