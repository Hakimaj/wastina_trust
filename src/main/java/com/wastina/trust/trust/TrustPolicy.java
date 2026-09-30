package com.wastina.trust.trust;

import java.math.BigDecimal;

public record TrustPolicy(
        String version,
        BigDecimal employmentWeight,
        BigDecimal guaranteeWeight,
        BigDecimal feedbackWeight,
        BigDecimal disputeWeight,
        BigDecimal verificationWeight,
        BigDecimal decayLambdaPerYear,
        int minimumEvidenceForMediumConfidence,
        int minimumEvidenceForHighConfidence) {

    public TrustPolicy {
        BigDecimal total = employmentWeight.add(guaranteeWeight)
                .add(feedbackWeight).add(disputeWeight).add(verificationWeight);
        if (total.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Trust policy weights must sum to 1");
        }
        if (version == null || version.isBlank() || decayLambdaPerYear.signum() < 0) {
            throw new IllegalArgumentException("Invalid trust policy");
        }
    }

    public static TrustPolicy v1() {
        return new TrustPolicy("1.0", new BigDecimal("0.25"), new BigDecimal("0.30"),
                new BigDecimal("0.15"), new BigDecimal("0.20"), new BigDecimal("0.10"),
                new BigDecimal("0.08"), 3, 8);
    }
}