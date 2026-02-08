package com.wastina.trust.controller;

import com.wastina.trust.dto.WorkerProfileRequestDto;
import com.wastina.trust.dto.WorkerProfileResponseDto;
import com.wastina.trust.service.WorkerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;

    @PutMapping("/{userId}/profile")
    public ResponseEntity<WorkerProfileResponseDto> createOrUpdateProfile(
            @PathVariable UUID userId,
            @RequestBody WorkerProfileRequestDto request) {
        WorkerProfileResponseDto response = workerProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<WorkerProfileResponseDto> getProfile(@PathVariable UUID userId) {
        WorkerProfileResponseDto response = workerProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        workerProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
