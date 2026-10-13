package com.wastina.trust.service;

import com.wastina.trust.dto.TrustScoreDto;
import com.wastina.trust.entity.IdentityStatus;
import com.wastina.trust.entity.TrustEvent;
import com.wastina.trust.entity.TrustScoreHistory;
import com.wastina.trust.entity.TrustScoreSnapshot;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.TrustEventRepository;
import com.wastina.trust.repository.TrustScoreHistoryRepository;
import com.wastina.trust.repository.TrustScoreSnapshotRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.trust.DeterministicTrustCalculator;
import com.wastina.trust.trust.TrustCalculationInput;
import com.wastina.trust.trust.TrustCalculationResult;
import com.wastina.trust.trust.TrustFact;
import com.wastina.trust.trust.TrustPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TrustScoreService {
    private final UserRepository userRepository;
    private final TrustEventRepository eventRepository;
    private final TrustScoreSnapshotRepository snapshotRepository;
    private final TrustScoreHistoryRepository historyRepository;
    private final DeterministicTrustCalculator calculator = new DeterministicTrustCalculator();

    @Transactional
    public TrustScoreDto recalculate(UUID userId, Instant asOf) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Instant calculationTime = asOf == null ? Instant.now() : asOf;
        List<TrustFact> facts = eventRepository.findBySubjectUserIdOrderByEffectiveAtAsc(userId).stream()
                .filter(event -> !event.getEffectiveAt().isAfter(calculationTime))
                .map(this::toFact)
                .toList();
        TrustCalculationResult result = calculator.calculate(
                new TrustCalculationInput(calculationTime, user.getIdentityStatus() == IdentityStatus.VERIFIED, facts),
                TrustPolicy.v1());
        TrustScoreSnapshot previous = snapshotRepository.findById(userId).orElse(null);
        TrustScoreSnapshot snapshot = previous == null ? new TrustScoreSnapshot() : previous;
        int previousScore = previous == null ? 0 : previous.getScore();
        snapshot.setUserId(userId);
        snapshot.setScore(result.score());
        snapshot.setInternalScore(result.internalScore());
        snapshot.setConfidence(result.confidence());
        snapshot.setEvidenceStatus(result.status());
        snapshot.setPolicyVersion(result.policyVersion());
        snapshot.setCalculatedAt(calculationTime);
        snapshotRepository.save(snapshot);
        if (previous == null || previousScore != result.score() || previous.getConfidence() != result.confidence()) {
            TrustScoreHistory history = new TrustScoreHistory();
            history.setUserId(userId);
            history.setPreviousScore(previousScore);
            history.setNewScore(result.score());
            history.setPreviousConfidence(previous == null ? result.confidence() : previous.getConfidence());
            history.setNewConfidence(result.confidence());
            history.setPolicyVersion(result.policyVersion());
            history.setReasonCode("POLICY_RECALCULATION");
            history.setCreatedAt(Instant.now());
            historyRepository.save(history);
        }
        List<String> breakdown = result.factors().stream()
                .map(factor -> factor.factor() + ": " + factor.weightedContribution().setScale(2, java.math.RoundingMode.HALF_UP))
                .toList();
        TrustScoreDto dto = new TrustScoreDto();
        dto.setTotalScore(result.score());
        dto.setWorkerId(userId.toString());
        dto.setScoreBreakdown(breakdown);
        dto.setConfidence(result.confidence().name());
        dto.setEvidenceStatus(result.status().name());
        dto.setPolicyVersion(result.policyVersion());
        dto.setEvidenceCount(result.evidenceCount());
        return dto;
    }

    private TrustFact toFact(TrustEvent event) {
        return new TrustFact(event.getEventId(), event.getEventType(), event.getSourceService(),
                event.getSubjectUserId(), event.getEffectiveAt(), event.getQuality().multiply(new java.math.BigDecimal("100")),
                event.isAdjudicated(), event.isPositive(), event.isNegative());
    }
}