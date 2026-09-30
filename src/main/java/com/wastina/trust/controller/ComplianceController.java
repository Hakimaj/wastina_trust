package com.wastina.trust.controller;

import com.wastina.trust.entity.*;
import com.wastina.trust.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ComplianceController {
    private final ComplianceService complianceService;

    @PostMapping("/complaints")
    public ResponseEntity<Complaint> submitComplaint(@RequestBody ComplaintRequest request) {
        return ResponseEntity.ok(complianceService.submitComplaint(request.complainantId(), request.subjectId(), request.category(),
                request.description(), request.relationshipType(), request.sourceEntityType(), request.sourceEntityId()));
    }

    @PostMapping("/compliance/complaints/{complaintId}/cases")
    public ResponseEntity<ComplianceCase> openCase(@PathVariable UUID complaintId, @RequestParam UUID subjectId) {
        return ResponseEntity.ok(complianceService.openCase(complaintId, subjectId));
    }

    @PostMapping("/compliance/cases/{caseId}/investigation")
    public ResponseEntity<ComplianceCase> startInvestigation(@PathVariable UUID caseId) {
        return ResponseEntity.ok(complianceService.startInvestigation(caseId));
    }

    @PostMapping("/compliance/cases/{caseId}/decision")
    public ResponseEntity<ComplianceCase> submitDecision(@PathVariable UUID caseId, @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(complianceService.submitDecision(caseId, request.role(), request.caseScopeGranted(),
                request.decisionType(), request.reason()));
    }

    @PostMapping("/compliance/cases/{caseId}/appeals")
    public ResponseEntity<Appeal> submitAppeal(@PathVariable UUID caseId, @RequestBody AppealRequest request) {
        return ResponseEntity.ok(complianceService.submitAppeal(caseId, request.appellantId(), request.grounds()));
    }

    @PostMapping("/compliance/cases/{caseId}/actions")
    public ResponseEntity<ComplianceAction> applyAction(@PathVariable UUID caseId, @RequestBody ActionRequest request) {
        return ResponseEntity.ok(complianceService.applyTemporaryAction(caseId, request.actorId(), request.type(),
                request.reason(), request.expiresAt()));
    }

    public record ComplaintRequest(UUID complainantId, UUID subjectId, ComplaintCategory category, String description,
                                   String relationshipType, String sourceEntityType, String sourceEntityId) {}

    public record DecisionRequest(ComplianceRole role, boolean caseScopeGranted, DecisionType decisionType, String reason) {}

    public record AppealRequest(UUID appellantId, AppealGrounds grounds) {}

    public record ActionRequest(UUID actorId, ComplianceActionType type, String reason, Instant expiresAt) {}
}
