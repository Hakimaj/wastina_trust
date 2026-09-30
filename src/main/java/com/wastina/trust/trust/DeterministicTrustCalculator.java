package com.wastina.trust.trust;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("null")
public final class DeterministicTrustCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public TrustCalculationResult calculate(TrustCalculationInput input, TrustPolicy policy) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(policy, "policy");
        List<TrustFact> facts = input.facts() == null ? List.of() : input.facts().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toMap(TrustFact::eventId, fact -> fact, (left, right) -> left),
                        map -> List.copyOf(map.values())));

        TrustFactorResult employment = factor("EMPLOYMENT_RELIABILITY", policy.employmentWeight(),
                facts, fact -> fact.eventType().startsWith("EMPLOYMENT_"), input.asOf(), false);
        TrustFactorResult guarantee = factor("GUARANTEE_RELIABILITY", policy.guaranteeWeight(),
                facts, fact -> fact.eventType().startsWith("GUARANTEE_"), input.asOf(), true);
        TrustFactorResult feedback = factor("VERIFIED_FEEDBACK", policy.feedbackWeight(),
                facts, fact -> fact.eventType().equals("VERIFIED_FEEDBACK"), input.asOf(), false);
        TrustFactorResult dispute = factor("DISPUTE_RECORD", policy.disputeWeight(),
                facts, fact -> fact.eventType().startsWith("DISPUTE_"), input.asOf(), true);
        TrustFactorResult verification = new TrustFactorResult("VERIFICATION_QUALITY",
                input.identityVerified() ? HUNDRED : BigDecimal.ZERO,
                input.identityVerified() ? policy.verificationWeight().multiply(HUNDRED) : BigDecimal.ZERO,
                input.identityVerified() ? 1 : 0);

        List<TrustFactorResult> factors = List.of(employment, guarantee, feedback, dispute, verification);
        BigDecimal internal = factors.stream().map(TrustFactorResult::weightedContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO).min(HUNDRED);
        int score = internal.setScale(0, RoundingMode.HALF_UP).intValueExact();
        int evidenceCount = facts.size() + verification.evidenceCount();
        TrustConfidence confidence = evidenceCount >= policy.minimumEvidenceForHighConfidence() ? TrustConfidence.HIGH
                : evidenceCount >= policy.minimumEvidenceForMediumConfidence() ? TrustConfidence.MEDIUM : TrustConfidence.LOW;
        boolean underReview = facts.stream().anyMatch(fact -> fact.eventType().equals("COMPLAINT_FILED"));
        TrustEvidenceStatus status = underReview ? TrustEvidenceStatus.UNDER_REVIEW
                : evidenceCount == 0 ? TrustEvidenceStatus.NO_DATA
                : confidence == TrustConfidence.LOW ? TrustEvidenceStatus.LIMITED_DATA
                : TrustEvidenceStatus.VERIFIED;
        return new TrustCalculationResult(score, internal, confidence, status, evidenceCount, factors, policy.version());
    }

    private TrustFactorResult factor(String name, BigDecimal weight, List<TrustFact> facts,
                                     Predicate<TrustFact> selector, Instant asOf, boolean adjudicatedOnly) {
        List<TrustFact> selected = facts.stream().filter(selector)
                .filter(fact -> !adjudicatedOnly || fact.adjudicated()).toList();
        if (selected.isEmpty()) {
            return new TrustFactorResult(name, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TrustFact fact : selected) {
            BigDecimal direction = fact.negative() ? new BigDecimal("-1") : fact.positive() ? BigDecimal.ONE : BigDecimal.ZERO;
            long days = Math.max(0, Duration.between(fact.effectiveAt(), asOf).toDays());
            BigDecimal recency = BigDecimal.ONE.subtract(new BigDecimal(days).divide(new BigDecimal("365"), 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.08"))).max(BigDecimal.ZERO);
            total = total.add(direction.multiply(fact.quality()).multiply(recency));
        }
        BigDecimal normalized = total.divide(new BigDecimal(selected.size()), 8, RoundingMode.HALF_UP)
                .add(HUNDRED).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(HUNDRED);
        return new TrustFactorResult(name, normalized, normalized.multiply(weight), selected.size());
    }
}