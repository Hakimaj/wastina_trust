package com.wastina.trust.controller;

import com.wastina.trust.dto.EmploymentCreateRequestDto;
import com.wastina.trust.dto.EmploymentRecordView;
import com.wastina.trust.dto.EmploymentVerificationView;
import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.EmploymentService;
import com.wastina.trust.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Employment records and employment verification.
 *
 * <p>This is the canonical employment endpoint family. Two changes from the audited original:
 * <ul>
 *   <li>The actor is always derived from the JWT. No endpoint takes a caller-supplied
 *       {@code requesterId} or {@code workerId}.</li>
 *   <li>Creating an employment record now requires active membership of the referenced
 *       organization, so a record cannot be fabricated against an arbitrary organization.</li>
 * </ul>
 *
 * <p>The legacy family ({@code POST /api/v1/workers/{id}/employment},
 * {@code POST /api/v1/employment/{id}/verification-requests},
 * {@code POST /api/v1/organizations/{id}/verification-requests/{id}/verify}) that returned raw JPA
 * entities and accepted caller-supplied actor ids has been removed. {@code /me/*} plus
 * {@code /organizations/{id}/employment-verification-requests/*} covers the same operations with
 * proper authorization and view DTOs.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Employment", description = "Employment records and employer verification")
public class EmploymentController {

    private final EmploymentService employmentService;
    private final CurrentUserResolver currentUserResolver;
    private final OrganizationService organizationService;

    public EmploymentController(EmploymentService employmentService,
                                CurrentUserResolver currentUserResolver,
                                OrganizationService organizationService) {
        this.employmentService = employmentService;
        this.currentUserResolver = currentUserResolver;
        this.organizationService = organizationService;
    }

    @GetMapping("/me/employment")
    @Transactional(readOnly = true)
    @Operation(summary = "List the caller's employment records")
    public List<EmploymentRecordView> listCurrentWorkerEmployment(@AuthenticationPrincipal Jwt jwt) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();
        return employmentService.listEmploymentForWorker(workerId).stream()
                .map(EmploymentRecordView::from)
                .toList();
    }

    @GetMapping("/me/employment/{employmentRecordId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Read one of the caller's employment records")
    public EmploymentRecordView getCurrentWorkerEmployment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID employmentRecordId) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();
        return EmploymentRecordView.from(
                employmentService.getEmploymentForWorker(workerId, employmentRecordId));
    }

    @PostMapping("/me/employment")
    @Transactional
    @Operation(summary = "Create an employment record for the caller")
    public ResponseEntity<EmploymentRecordView> createCurrentWorkerEmployment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EmploymentCreateRequestDto request) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();

        // The caller must actually belong to the organization they are claiming to work for.
        organizationService.requireActiveMembership(request.organizationId(), workerId);

        EmploymentRecord record = employmentService.createEmploymentRecord(workerId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/me/employment/" + record.getId()))
                .body(EmploymentRecordView.from(record));
    }

    @GetMapping("/me/employment-verification-requests")
    @Transactional(readOnly = true)
    @Operation(summary = "List verification requests the caller submitted")
    public List<EmploymentVerificationView> listCurrentWorkerVerificationRequests(
            @AuthenticationPrincipal Jwt jwt) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();
        return employmentService.listVerificationRequestsForWorker(workerId).stream()
                .map(EmploymentVerificationView::from)
                .toList();
    }

    @PostMapping("/me/employment/{employmentRecordId}/verification-requests")
    @Transactional
    @Operation(summary = "Ask the record's organization to verify an employment record")
    public ResponseEntity<EmploymentVerificationView> requestCurrentWorkerVerification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID employmentRecordId) {
        UUID workerId = currentUserResolver.requireUser(jwt).getId();
        EmploymentRecord record = employmentService.getEmploymentForWorker(workerId, employmentRecordId);
        EmploymentVerificationRequest request = employmentService.requestVerification(
                employmentRecordId, workerId, record.getOrganization().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EmploymentVerificationView.from(request));
    }

    @GetMapping("/organizations/{organizationId}/employment-verification-requests")
    @Transactional(readOnly = true)
    @Operation(summary = "List verification requests addressed to an organization")
    public List<EmploymentVerificationView> listOrganizationVerificationRequests(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        UUID verifierId = currentUserResolver.requireUser(jwt).getId();
        return employmentService.listVerificationRequestsForOrganization(verifierId, organizationId).stream()
                .map(EmploymentVerificationView::from)
                .toList();
    }

    @PostMapping("/organizations/{organizationId}/employment-verification-requests/{requestId}/review")
    @Transactional
    @Operation(summary = "Move a verification request into review")
    public EmploymentVerificationView beginEmploymentReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID requestId) {
        UUID verifierId = currentUserResolver.requireUser(jwt).getId();
        return EmploymentVerificationView.from(
                employmentService.beginVerificationReview(verifierId, organizationId, requestId));
    }

    /**
     * Verifies a request. Previously reachable at
     * {@code /organizations/{id}/verification-requests/{id}/verify}; the name now matches its
     * sibling {@code /review} operation.
     */
    @PostMapping("/organizations/{organizationId}/employment-verification-requests/{requestId}/verify")
    @Transactional
    @Operation(summary = "Confirm an employment record as verified")
    public EmploymentVerificationView verifyEmployment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID requestId) {
        UUID verifierId = currentUserResolver.requireUser(jwt).getId();
        return EmploymentVerificationView.from(
                employmentService.verifyEmployment(verifierId, organizationId, requestId));
    }
}