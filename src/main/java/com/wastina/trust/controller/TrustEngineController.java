package com.wastina.trust.controller;

import com.wastina.trust.dto.TrustScoreDto;
import com.wastina.trust.service.TrustEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trust-engine")
@RequiredArgsConstructor
public class TrustEngineController {

    private final TrustEngineService trustEngineService;

    // This endpoint can be called by Employers to check a worker's score
    @GetMapping("/{workerId}/score")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TrustScoreDto> getTrustScore(@PathVariable UUID workerId) {
        try {
            TrustScoreDto trustScore = trustEngineService.calculateTrustScore(workerId);
            return ResponseEntity.ok(trustScore);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
