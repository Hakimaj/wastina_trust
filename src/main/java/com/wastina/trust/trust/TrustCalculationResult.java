package com.wastina.trust.trust;

import java.math.BigDecimal;
import java.util.List;

public record TrustCalculationResult(
        int score,
        BigDecimal internalScore,
        TrustConfidence confidence,
        TrustEvidenceStatus status,
        int evidenceCount,
        List<TrustFactorResult> factors,
        String policyVersion) {
}