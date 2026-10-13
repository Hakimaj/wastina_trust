package com.wastina.trust.controller;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import com.wastina.trust.service.EmployerProfileService;
import com.wastina.trust.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
public class EmployerProfileController {

    private final EmployerProfileService employerProfileService;
    private final CurrentUserResolver currentUserResolver;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<EmployerProfileResponseDto> createOrUpdateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody EmployerProfileRequestDto request) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        EmployerProfileResponseDto response = employerProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<EmployerProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        EmployerProfileResponseDto response = employerProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        employerProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
