package com.wastina.trust.controller;

import com.wastina.trust.dto.EmployerProfileRequestDto;
import com.wastina.trust.dto.EmployerProfileResponseDto;
import com.wastina.trust.service.EmployerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
public class EmployerProfileController {

    private final EmployerProfileService employerProfileService;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<EmployerProfileResponseDto> createOrUpdateProfile(
            @PathVariable UUID userId,
            @RequestBody EmployerProfileRequestDto request) {
        EmployerProfileResponseDto response = employerProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<EmployerProfileResponseDto> getProfile(@PathVariable UUID userId) {
        EmployerProfileResponseDto response = employerProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        employerProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
