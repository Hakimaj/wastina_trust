package com.wastina.trust.trust;

import java.time.Instant;
import java.util.List;

public record TrustCalculationInput(
        Instant asOf,
        boolean identityVerified,
        List<TrustFact> facts) {
}