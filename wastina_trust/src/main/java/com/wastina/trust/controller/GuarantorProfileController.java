package com.wastina.trust.controller;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.service.GuarantorProfileService;
import com.wastina.trust.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/guarantors")
@RequiredArgsConstructor
public class GuarantorProfileController {

    private final GuarantorProfileService guarantorProfileService;
    private final CurrentUserResolver currentUserResolver;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<GuarantorProfileResponseDto> createOrUpdateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody GuarantorProfileRequestDto request) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        GuarantorProfileResponseDto response = guarantorProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<GuarantorProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        GuarantorProfileResponseDto response = guarantorProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        currentUserResolver.requireMatchingUserId(jwt, userId);
        guarantorProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
