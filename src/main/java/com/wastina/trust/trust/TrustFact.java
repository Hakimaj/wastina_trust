package com.wastina.trust.trust;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrustFact(
        UUID eventId,
        String eventType,
        String sourceService,
        UUID sourceEntityId,
        Instant effectiveAt,
        BigDecimal quality,
        boolean adjudicated,
        boolean positive,
        boolean negative) {
}