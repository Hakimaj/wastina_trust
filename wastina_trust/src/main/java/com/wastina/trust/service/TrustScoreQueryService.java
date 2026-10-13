package com.wastina.trust.service;

import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.dto.TrustScoreHistoryView;
import com.wastina.trust.entity.TrustScoreHistory;
import com.wastina.trust.entity.TrustScoreSnapshot;
import com.wastina.trust.dto.TrustScoreView;
import com.wastina.trust.repository.TrustScoreHistoryRepository;
import com.wastina.trust.repository.TrustScoreSnapshotRepository;
import com.wastina.trust.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only access to a stored trust score.
 *
 * <p>The audit found {@code GET /api/trust-engine/{workerId}/score} recalculated the score on every
 * read, which wrote snapshot and history rows on a {@code GET} request. That breaks caching, makes
 * reads non-idempotent, and lets an unrelated caller trigger a recalculation.
 *
 * <p>Recalculation is now separated from reading: {@link TrustScoreService#recalculate} persists a
 * score, and this service only reports what is already stored. If no score has been computed yet,
 * the read returns {@code 404} rather than silently computing one.
 */
@Service
public class TrustScoreQueryService {

    private final TrustScoreSnapshotRepository snapshotRepository;
    private final TrustScoreHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public TrustScoreQueryService(TrustScoreSnapshotRepository snapshotRepository,
                                  TrustScoreHistoryRepository historyRepository,
                                  UserRepository userRepository) {
        this.snapshotRepository = snapshotRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    /** The stored score for a user, or a 404 when none has been calculated. */
    @Transactional(readOnly = true)
    public TrustScoreView getStoredScore(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("No account was found for " + userId + ".");
        }
        return snapshotRepository.findById(userId)
                .map(TrustScoreView::from)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No trust score has been calculated for this account yet."));
    }

    /** Score history, newest first, for the trend chart on the trust page. */
    @Transactional(readOnly = true)
    public List<TrustScoreHistoryView> getHistory(UUID userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(safeLimit)
                .map(TrustScoreHistoryView::from)
                .toList();
    }
}