package com.wastina.trust.dto;

import com.wastina.trust.entity.TrustScoreSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * A stored trust score.
 *
 * <p>Unlike {@code TrustScoreDto}, which is a calculation result carrying a factor breakdown, this is
 * the persisted snapshot: what was last computed, when, and under which policy.
 */
@Schema(name = "TrustScore")
public record TrustScoreView(
        @Schema(description = "Identifier of the scored account")
        UUID userId,

        @Schema(description = "Overall score, 0-100")
        int score,

        @Schema(description = "Unrounded score before policy capping")
        java.math.BigDecimal internalScore,

        @Schema(description = "Confidence in the score")
        String confidence,

        @Schema(description = "Quality of the supporting evidence")
        String evidenceStatus,

        @Schema(description = "Policy version used for the calculation")
        String policyVersion,

        @Schema(description = "When the score was calculated")
        Instant calculatedAt) {

    public static TrustScoreView from(TrustScoreSnapshot snapshot) {
        return new TrustScoreView(
                snapshot.getUserId(),
                snapshot.getScore(),
                snapshot.getInternalScore(),
                snapshot.getConfidence() == null ? null : snapshot.getConfidence().name(),
                snapshot.getEvidenceStatus() == null ? null : snapshot.getEvidenceStatus().name(),
                snapshot.getPolicyVersion(),
                snapshot.getCalculatedAt());
    }
}