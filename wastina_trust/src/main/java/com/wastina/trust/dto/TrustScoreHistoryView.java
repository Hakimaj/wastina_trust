package com.wastina.trust.dto;

import com.wastina.trust.entity.TrustScoreHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** One entry in a trust score's history. */
@Schema(name = "TrustScoreHistory")
public record TrustScoreHistoryView(
        @Schema(description = "History entry identifier")
        UUID id,

        @Schema(description = "Identifier of the scored account")
        UUID userId,

        @Schema(description = "Score before this change")
        int previousScore,

        @Schema(description = "Score after this change")
        int newScore,

        @Schema(description = "Confidence before this change")
        String previousConfidence,

        @Schema(description = "Confidence after this change")
        String newConfidence,

        @Schema(description = "Policy version applied")
        String policyVersion,

        @Schema(description = "Why the score changed")
        String reasonCode,

        @Schema(description = "When the change was recorded")
        Instant createdAt) {

    public static TrustScoreHistoryView from(TrustScoreHistory history) {
        return new TrustScoreHistoryView(
                history.getId(),
                history.getUserId(),
                history.getPreviousScore(),
                history.getNewScore(),
                history.getPreviousConfidence() == null ? null : history.getPreviousConfidence().name(),
                history.getNewConfidence() == null ? null : history.getNewConfidence().name(),
                history.getPolicyVersion(),
                history.getReasonCode(),
                history.getCreatedAt());
    }
}