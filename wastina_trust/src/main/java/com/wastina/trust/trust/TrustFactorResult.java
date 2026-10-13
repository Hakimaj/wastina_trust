package com.wastina.trust.trust;

import java.math.BigDecimal;

public record TrustFactorResult(
        String factor,
        BigDecimal normalizedValue,
        BigDecimal weightedContribution,
        int evidenceCount) {
}