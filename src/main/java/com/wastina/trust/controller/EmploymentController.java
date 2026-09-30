package com.wastina.trust.controller;

import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.service.EmploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmploymentController {
    private final EmploymentService employmentService;

    @PostMapping("/workers/{workerId}/employment")
    public ResponseEntity<EmploymentRecord> createEmploymentRecord(
            @PathVariable UUID workerId,
            @RequestParam UUID organizationId,
            @RequestParam String jobTitleEn) {
        return ResponseEntity.ok(employmentService.createEmploymentRecord(workerId, organizationId, jobTitleEn));
    }

    @PostMapping("/employment/{employmentRecordId}/verification-requests")
    public ResponseEntity<EmploymentVerificationRequest> requestVerification(
            @PathVariable UUID employmentRecordId,
            @RequestParam UUID requesterId,
            @RequestParam UUID organizationId) {
        return ResponseEntity.ok(employmentService.requestVerification(employmentRecordId, requesterId, organizationId));
    }

    @PostMapping("/organizations/{organizationId}/verification-requests/{requestId}/verify")
    public ResponseEntity<EmploymentVerificationRequest> verifyEmployment(
            @PathVariable UUID organizationId,
            @PathVariable UUID requestId,
            @RequestParam UUID verifierUserId) {
        return ResponseEntity.ok(employmentService.verifyEmployment(verifierUserId, organizationId, requestId));
    }
}
