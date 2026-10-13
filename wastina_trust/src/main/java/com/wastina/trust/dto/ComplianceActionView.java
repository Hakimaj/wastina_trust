package com.wastina.trust.dto;

import com.wastina.trust.entity.ComplianceAction;
import com.wastina.trust.entity.ComplianceActionStatus;
import com.wastina.trust.entity.ComplianceActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** A temporary compliance action applied to a case. */
@Schema(name = "ComplianceAction")
public record ComplianceActionView(
        @Schema(description = "Action identifier")
        UUID id,

        @Schema(description = "Case the action applies to")
        UUID caseId,

        @Schema(description = "Action category")
        ComplianceActionType type,

        @Schema(description = "Action state")
        ComplianceActionStatus status,

        @Schema(description = "Reason recorded for the action")
        String reason,

        @Schema(description = "Identifier of the account that applied the action")
        UUID appliedBy,

        @Schema(description = "When the action started")
        Instant startsAt,

        @Schema(description = "When the action expires")
        Instant expiresAt,

        @Schema(description = "When the action is due for review")
        Instant reviewAt) {

    public static ComplianceActionView from(ComplianceAction action) {
        return new ComplianceActionView(
                action.getId(),
                action.getComplianceCase() == null ? null : action.getComplianceCase().getId(),
                action.getType(),
                action.getStatus(),
                action.getReason(),
                action.getAppliedBy(),
                action.getStartsAt(),
                action.getExpiresAt(),
                action.getReviewAt());
    }
}