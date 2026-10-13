package com.wastina.trust.controller;

import com.wastina.trust.dto.AppealView;
import com.wastina.trust.dto.ComplaintView;
import com.wastina.trust.dto.ComplianceActionView;
import com.wastina.trust.dto.ComplianceCaseView;
import com.wastina.trust.entity.Appeal;
import com.wastina.trust.entity.Complaint;
import com.wastina.trust.entity.ComplianceAction;
import com.wastina.trust.entity.ComplianceCase;
import com.wastina.trust.entity.ComplianceRole;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.ComplianceService;
import com.wastina.trust.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Complaints, compliance cases, decisions, appeals and actions.
 *
 * <p>The audit found every endpoint here accepted a caller-supplied actor identifier, and that
 * {@code POST /cases/{id}/decision} trusted a body-supplied {@code ComplianceRole} plus a boolean
 * {@code caseScopeGranted} as its authorization decision. Both are fixed:
 * <ul>
 *   <li>The complainant, appellant and actor are always the authenticated caller.</li>
 *   <li>The {@link ComplianceRole} is derived from the caller's {@code ROLE_ADMIN} authority; a
 *       client cannot claim to be a compliance manager.</li>
 *   <li>{@code caseScopeGranted} is computed server-side from actual case scope.</li>
 *   <li>Responses are view DTOs, so nested {@code User} entities (and their email and phone
 *       numbers) are no longer serialized.</li>
 * </ul>
 *
 * <p>Read endpoints were added: previously the domain was write-only, so no compliance UI could
 * exist. All reads require an administrative authority.
 */
@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "Compliance", description = "Complaints and dispute resolution")
public class ComplianceController {

    private static final int MAX_PAGE_SIZE = 200;

    private final ComplianceService complianceService;
    private final CurrentUserResolver currentUserResolver;

    public ComplianceController(ComplianceService complianceService,
                                CurrentUserResolver currentUserResolver) {
        this.complianceService = complianceService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/complaints")
    @Operation(summary = "Submit a complaint as the authenticated caller")
    public ResponseEntity<ComplaintView> submitComplaint(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ComplaintRequest request) {
        UUID complainantId = currentUserResolver.requireUser(jwt).getId();
        Complaint complaint = complianceService.submitComplaint(
                complainantId, request.subjectId(), request.category(), request.description(),
                request.relationshipType(), request.sourceEntityType(), request.sourceEntityId());
        return ResponseEntity
                .created(URI.create("/api/v1/compliance/complaints/" + complaint.getId()))
                .body(ComplaintView.from(complaint));
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List complaints (administrator only, paged)")
    public ResponseEntity<PageResponse<ComplaintView>> listComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var result = complianceService.listComplaints(
                PageRequest.of(Math.max(page, 0), pageSize));
        return ResponseEntity.ok(PageResponse.of(result,
                result.getContent().stream().map(ComplaintView::from).toList()));
    }

    @PostMapping("/complaints/{complaintId}/cases")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Open a compliance case from a complaint")
    public ResponseEntity<ComplianceCaseView> openCase(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID complaintId,
            @Valid @RequestBody OpenCaseRequest request) {
        UUID subjectId = request.subjectId();
        ComplianceCase complianceCase = complianceService.openCase(complaintId, subjectId);
        return ResponseEntity
                .created(URI.create("/api/v1/compliance/cases/" + complianceCase.getId()))
                .body(ComplianceCaseView.from(complianceCase));
    }

    @PostMapping("/cases/{caseId}/investigation")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Move a case into investigation")
    public ComplianceCaseView startInvestigation(@PathVariable UUID caseId) {
        return ComplianceCaseView.from(complianceService.startInvestigation(caseId));
    }

    /**
     * Records a decision. The deciding role and case scope are derived server-side; the request body
     * carries only the decision itself.
     */
    @PostMapping("/cases/{caseId}/decision")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Record a decision on a case (authority derived server-side)")
    public ComplianceCaseView submitDecision(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @Valid @RequestBody DecisionRequest request) {
        User decider = currentUserResolver.requireUser(jwt);
        ComplianceRole role = complianceService.resolveRoleFor(decider);
        boolean caseScopeGranted = complianceService.hasCaseScope(decider, caseId);

        return ComplianceCaseView.from(complianceService.submitDecision(
                caseId, role, caseScopeGranted, request.decisionType(), request.reason()));
    }

    @PostMapping("/cases/{caseId}/appeals")
    @Operation(summary = "Appeal a decided case as the authenticated caller")
    public ResponseEntity<AppealView> submitAppeal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @Valid @RequestBody AppealRequest request) {
        UUID appellantId = currentUserResolver.requireUser(jwt).getId();
        Appeal appeal = complianceService.submitAppeal(caseId, appellantId, request.grounds());
        return ResponseEntity
                .created(URI.create("/api/v1/compliance/cases/" + caseId + "/appeals"))
                .body(AppealView.from(appeal));
    }

    @PostMapping("/cases/{caseId}/actions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Apply a temporary compliance action as the authenticated caller")
    public ResponseEntity<ComplianceActionView> applyAction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @Valid @RequestBody ActionRequest request) {
        UUID actorId = currentUserResolver.requireUser(jwt).getId();
        ComplianceAction action = complianceService.applyTemporaryAction(
                caseId, actorId, request.type(), request.reason(), request.expiresAt());
        return ResponseEntity
                .created(URI.create("/api/v1/compliance/cases/" + caseId + "/actions"))
                .body(ComplianceActionView.from(action));
    }

    @GetMapping("/cases")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List compliance cases (administrator only, paged)")
    public ResponseEntity<PageResponse<ComplianceCaseView>> listCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var result = complianceService.listCases(PageRequest.of(Math.max(page, 0), pageSize));
        return ResponseEntity.ok(PageResponse.of(result,
                result.getContent().stream().map(ComplianceCaseView::from).toList()));
    }

    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Read one compliance case")
    public ComplianceCaseView getCase(@PathVariable UUID caseId) {
        return ComplianceCaseView.from(complianceService.getCase(caseId));
    }

    @GetMapping("/cases/{caseId}/appeals")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List appeals against a case")
    public java.util.List<AppealView> listAppeals(@PathVariable UUID caseId) {
        return complianceService.listAppeals(caseId).stream().map(AppealView::from).toList();
    }

    @GetMapping("/cases/{caseId}/actions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List actions applied to a case")
    public java.util.List<ComplianceActionView> listActions(@PathVariable UUID caseId) {
        return complianceService.listActions(caseId).stream().map(ComplianceActionView::from).toList();
    }

    public record ComplaintRequest(
            @NotNull(message = "is required") UUID subjectId,

            @NotNull(message = "is required")
            com.wastina.trust.entity.ComplaintCategory category,

            @NotNull(message = "is required")
            @Size(min = 20, max = 4000, message = "must be between 20 and 4000 characters")
            String description,

            @Size(max = 40, message = "must be at most 40 characters")
            String relationshipType,

            @Size(max = 60, message = "must be at most 60 characters")
            String sourceEntityType,

            @Size(max = 100, message = "must be at most 100 characters")
            String sourceEntityId) {
    }

    public record OpenCaseRequest(
            @NotNull(message = "is required") UUID subjectId) {
    }

    /** Carries only the decision itself; role and scope are derived server-side. */
    public record DecisionRequest(
            @NotNull(message = "is required")
            com.wastina.trust.entity.DecisionType decisionType,

            @Size(max = 4000, message = "must be at most 4000 characters")
            String reason) {
    }

    public record AppealRequest(
            @NotNull(message = "is required")
            com.wastina.trust.entity.AppealGrounds grounds) {
    }

    public record ActionRequest(
            @NotNull(message = "is required")
            com.wastina.trust.entity.ComplianceActionType type,

            @NotNull(message = "is required")
            @Size(min = 1, max = 1000, message = "must be between 1 and 1000 characters")
            String reason,

            java.time.Instant expiresAt) {
    }
}