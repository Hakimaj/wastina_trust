package com.wastina.trust.trust;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicTrustCalculatorTest {
    private static final Instant AS_OF = Instant.parse("2026-09-30T00:00:00Z");
    private final DeterministicTrustCalculator calculator = new DeterministicTrustCalculator();

    @Test
    void coldStartIsNeutralButLowConfidence() {
        TrustCalculationResult result = calculator.calculate(new TrustCalculationInput(AS_OF, false, List.of()), TrustPolicy.v1());

        assertEquals(0, result.score());
        assertEquals(TrustConfidence.LOW, result.confidence());
        assertEquals(TrustEvidenceStatus.NO_DATA, result.status());
    }

    @Test
    void complaintAloneDoesNotLowerScoreButIsVisibleAsReviewStatus() {
        TrustFact complaint = fact("COMPLAINT_FILED", false, false, false, "2026-09-01T00:00:00Z");

        TrustCalculationResult result = calculator.calculate(new TrustCalculationInput(AS_OF, false, List.of(complaint)), TrustPolicy.v1());

        assertEquals(0, result.score());
        assertEquals(TrustEvidenceStatus.UNDER_REVIEW, result.status());
    }

    @Test
    void duplicateEventHasOneBusinessEffect() {
        TrustFact completion = fact("GUARANTEE_COMPLETED", false, true, false, "2026-09-01T00:00:00Z");
        TrustCalculationResult once = calculator.calculate(new TrustCalculationInput(AS_OF, false, List.of(completion)), TrustPolicy.v1());
        TrustCalculationResult twice = calculator.calculate(new TrustCalculationInput(AS_OF, false, List.of(completion, completion)), TrustPolicy.v1());

        assertEquals(once.score(), twice.score());
        assertEquals(once.evidenceCount(), twice.evidenceCount());
    }

    @Test
    void identityEvidenceContributesWithoutMakingTrustPerfect() {
        TrustCalculationResult result = calculator.calculate(new TrustCalculationInput(AS_OF, true, List.of()), TrustPolicy.v1());

        assertEquals(10, result.score());
        assertEquals(TrustConfidence.LOW, result.confidence());
        assertEquals(TrustEvidenceStatus.LIMITED_DATA, result.status());
    }

    private TrustFact fact(String type, boolean adjudicated, boolean positive, boolean negative, String effectiveAt) {
        return new TrustFact(UUID.randomUUID(), type, "test", UUID.randomUUID(), Instant.parse(effectiveAt),
                new BigDecimal("100"), adjudicated, positive, negative);
    }
}