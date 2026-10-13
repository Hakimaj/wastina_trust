package com.wastina.trust.dto;

import com.wastina.trust.entity.Appeal;
import com.wastina.trust.entity.AppealDecisionType;
import com.wastina.trust.entity.AppealGrounds;
import com.wastina.trust.entity.AppealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** An appeal against a compliance case decision. */
@Schema(name = "Appeal")
public record AppealView(
        @Schema(description = "Appeal identifier")
        UUID id,

        @Schema(description = "Case being appealed")
        UUID caseId,

        @Schema(description = "Identifier of the appellant")
        UUID appellantUserId,

        @Schema(description = "Grounds for the appeal")
        AppealGrounds grounds,

        @Schema(description = "Appeal state")
        AppealStatus status,

        @Schema(description = "Outcome, once resolved")
        AppealDecisionType decisionType,

        @Schema(description = "Explanation recorded with the appeal")
        String explanation,

        @Schema(description = "When the appeal was submitted")
        Instant submittedAt,

        @Schema(description = "When the appeal was resolved, if it has been")
        Instant resolvedAt) {

    public static AppealView from(Appeal appeal) {
        return new AppealView(
                appeal.getId(),
                appeal.getComplianceCase() == null ? null : appeal.getComplianceCase().getId(),
                appeal.getAppellantUserId(),
                appeal.getGrounds(),
                appeal.getStatus(),
                appeal.getDecisionType(),
                appeal.getExplanation(),
                appeal.getSubmittedAt(),
                appeal.getResolvedAt());
    }
}