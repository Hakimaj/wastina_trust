package com.wastina.trust.controller;

import com.wastina.trust.dto.GuarantorProfileRequestDto;
import com.wastina.trust.dto.GuarantorProfileResponseDto;
import com.wastina.trust.service.GuarantorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/guarantors")
@RequiredArgsConstructor
public class GuarantorProfileController {

    private final GuarantorProfileService guarantorProfileService;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<GuarantorProfileResponseDto> createOrUpdateProfile(
            @PathVariable UUID userId,
            @RequestBody GuarantorProfileRequestDto request) {
        GuarantorProfileResponseDto response = guarantorProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<GuarantorProfileResponseDto> getProfile(@PathVariable UUID userId) {
        GuarantorProfileResponseDto response = guarantorProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        guarantorProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
