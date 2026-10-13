package com.wastina.trust.controller;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import com.wastina.trust.service.WorkerProfileService;
import com.wastina.trust.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;
    private final CurrentUserResolver currentUserResolver;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<WorkerProfileResponseDto> createOrUpdateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody WorkerProfileRequestDto request) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        WorkerProfileResponseDto response = workerProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<WorkerProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        WorkerProfileResponseDto response = workerProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        workerProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
