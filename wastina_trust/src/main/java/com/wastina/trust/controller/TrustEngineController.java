package com.wastina.trust.controller;

import com.wastina.trust.dto.TrustScoreHistoryView;
import com.wastina.trust.dto.TrustScoreView;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.TrustScoreQueryService;
import com.wastina.trust.service.TrustScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trust score reads and explicit recalculation.
 *
 * <p>The audit found {@code GET /score} recalculated and persisted a score on every read, which made
 * a {@code GET} mutate state and let any authenticated caller trigger a recalculation for any
 * account. Changes:
 * <ul>
 *   <li>{@code GET} is now read-only. It reports the stored snapshot, or {@code 404} when none has
 *       been calculated.</li>
 *   <li>Reading is restricted to the account itself, or to an administrator.</li>
 *   <li>Recalculation moved to an explicit {@code POST}, so it is no longer a side effect of
 *       browsing the page.</li>
 *   <li>Score history is exposed for the trend chart.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/trust-score")
@Tag(name = "Trust", description = "Trust scores")
public class TrustEngineController {

    private final TrustScoreQueryService trustScoreQueryService;
    private final TrustScoreService trustScoreService;
    private final CurrentUserResolver currentUserResolver;

    public TrustEngineController(TrustScoreQueryService trustScoreQueryService,
                                 TrustScoreService trustScoreService,
                                 CurrentUserResolver currentUserResolver) {
        this.trustScoreQueryService = trustScoreQueryService;
        this.trustScoreService = trustScoreService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me")
    @Operation(summary = "Read the caller's stored trust score")
    public TrustScoreView getOwnScore(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return trustScoreQueryService.getStoredScore(userId);
    }

    @GetMapping("/me/history")
    @Operation(summary = "Read the caller's trust score history, newest first")
    public List<TrustScoreHistoryView> getOwnHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return trustScoreQueryService.getHistory(userId, limit);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Read a stored trust score (self or administrator only)")
    public TrustScoreView getScore(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {
        requireSelfOrAdministrator(jwt, userId);
        return trustScoreQueryService.getStoredScore(userId);
    }

    @GetMapping("/{userId}/history")
    @Operation(summary = "Read trust score history (self or administrator only)")
    public List<TrustScoreHistoryView> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        requireSelfOrAdministrator(jwt, userId);
        return trustScoreQueryService.getHistory(userId, limit);
    }

    /**
     * Explicitly recalculates and persists the caller's score.
     *
     * <p>This is the write that the old {@code GET} performed implicitly.
     */
    @PostMapping("/me/recalculate")
    @Operation(summary = "Recalculate and persist the caller's trust score")
    public TrustScoreView recalculate(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        trustScoreService.recalculate(userId, null);
        return trustScoreQueryService.getStoredScore(userId);
    }

    private void requireSelfOrAdministrator(Jwt jwt, UUID userId) {
        User caller = currentUserResolver.requireUser(jwt);
        if (caller.getId().equals(userId)) {
            return;
        }
        boolean administrator = caller.getRoles().stream()
                .anyMatch(role -> role.getName() == com.wastina.trust.entity.ERole.ROLE_ADMIN);
        if (!administrator) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "You may only read your own trust score.");
        }
    }
}