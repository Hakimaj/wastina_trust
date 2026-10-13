package com.wastina.trust.controller;

import com.wastina.trust.dto.OrganizationInvitationView;
import com.wastina.trust.dto.OrganizationMembershipView;
import com.wastina.trust.dto.OrganizationSummaryView;
import com.wastina.trust.entity.Organization;
import com.wastina.trust.entity.OrganizationInvitation;
import com.wastina.trust.entity.OrganizationInvitationStatus;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.OrganizationInvitationRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organization and membership endpoints.
 *
 * <p>All endpoints derive the acting user from the validated JWT. No endpoint accepts a
 * caller-supplied actor or member identifier, so a user cannot act on behalf of another account.
 *
 * <p>{@code GET /api/v1/organizations} is paged because the member list of a large employer grows
 * without bound; it follows the shared {@code page}/{@code size}/{@code sort} contract.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization and membership lifecycle")
public class OrganizationController {

    private static final int MAX_PAGE_SIZE = 200;

    private final OrganizationService organizationService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final OrganizationInvitationRepository invitationRepository;

    public OrganizationController(OrganizationService organizationService,
                                  CurrentUserResolver currentUserResolver,
                                  UserRepository userRepository,
                                  OrganizationInvitationRepository invitationRepository) {
        this.organizationService = organizationService;
        this.currentUserResolver = currentUserResolver;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
    }

    @PostMapping
    @Operation(summary = "Create an organization; the caller becomes its OWNER")
    public ResponseEntity<OrganizationSummaryView> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrganizationRequest request) {
        User creator = currentUserResolver.requireUser(jwt);
        Organization organization = organizationService.createOrganization(
                creator, request.name(), request.legalName(), request.description());
        return ResponseEntity
                .created(URI.create("/api/v1/me/organizations/" + organization.getId()))
                .body(OrganizationSummaryView.from(organization, true, true));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Read an organization the caller is a member of")
    public ResponseEntity<OrganizationSummaryView> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        User user = currentUserResolver.requireUser(jwt);
        Organization organization = organizationService.requireOrganization(organizationId);
        organizationService.requireMembership(organization, user);
        var membership = organizationService.findActiveMembership(organizationId, user.getId()).orElse(null);
        boolean canManage = membership != null && membership.canManageMemberships();
        return ResponseEntity.ok(OrganizationSummaryView.from(organization, canManage, false));
    }

    @PostMapping("/join")
    @Operation(summary = "Join an organization using an invitation code")
    @Transactional
    public ResponseEntity<OrganizationMembershipView> join(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JoinOrganizationRequest request) {
        User user = currentUserResolver.requireUser(jwt);
        OrganizationMembership membership = organizationService.redeemInvitation(request.joinCode(), user);
        return ResponseEntity
                .created(URI.create("/api/v1/me/organizations/" + membership.getOrganization().getId()))
                .body(OrganizationMembershipView.from(membership));
    }

    @DeleteMapping("/{organizationId}/membership")
    @Operation(summary = "Leave an organization")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        User user = currentUserResolver.requireUser(jwt);
        organizationService.leaveOrganization(organizationId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{organizationId}/members")
    @Operation(summary = "List the members of an organization (paged)")
    @Transactional(readOnly = true)
    public ResponseEntity<com.wastina.trust.web.PageResponse<OrganizationMembershipView>> listMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "role") String sort) {
        User user = currentUserResolver.requireUser(jwt);
        Organization organization = organizationService.requireOrganization(organizationId);
        organizationService.requireMembership(organization, user);

        Sort sortSpec = parseSort(sort);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // Sorted and windowed in memory because the membership table is small per organization and
        // the repository has no derived query for this ordering.
        var members = organizationService.listMembers(organizationId).stream()
                .sorted(comparatorFor(sortSpec))
                .toList();

        int from = (int) Math.min((long) page * pageSize, members.size());
        int to = Math.min(from + pageSize, members.size());
        java.util.List<OrganizationMembershipView> window =
                members.subList(from, to).stream().map(OrganizationMembershipView::from).toList();

        return ResponseEntity.ok(com.wastina.trust.web.PageResponse.of(window, page, pageSize, members.size()));
    }

    private java.util.Comparator<OrganizationMembership> comparatorFor(Sort sortSpec) {
        java.util.Comparator<OrganizationMembership> comparator =
                java.util.Comparator.comparing(OrganizationMembership::getRole);
        for (Sort.Order order : sortSpec) {
            if ("status".equals(order.getProperty())) {
                comparator = java.util.Comparator.comparing(OrganizationMembership::getStatus);
            }
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
        }
        return comparator;
    }

    @PatchMapping("/{organizationId}/members/{memberUserId}")
    @Operation(summary = "Change a member's role")
    public ResponseEntity<OrganizationMembershipView> updateMemberRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID memberUserId,
            @Valid @RequestBody UpdateMembershipRequest request) {
        User user = currentUserResolver.requireUser(jwt);
        OrganizationMembership updated = organizationService.updateMembershipRole(
                organizationId, memberUserId, request.role(), user);
        return ResponseEntity.ok(OrganizationMembershipView.from(updated));
    }

    @PostMapping("/{organizationId}/invitations")
    @Operation(summary = "Create a single-use invitation; the join code is returned only once")
    public ResponseEntity<OrganizationInvitationView> createInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateInvitationRequest request) {
        User user = currentUserResolver.requireUser(jwt);
        var created = organizationService.createInvitation(organizationId, user, request.joinCode());
        return ResponseEntity
                .created(URI.create("/api/v1/organizations/" + organizationId + "/invitations"))
                .body(OrganizationInvitationView.from(created.invitation(), created.joinCode()));
    }

    @GetMapping("/{organizationId}/invitations")
    @Operation(summary = "List active invitations (codes are never returned again)")
    public ResponseEntity<List<OrganizationInvitationView>> listInvitations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        User user = currentUserResolver.requireUser(jwt);
        Organization organization = organizationService.requireOrganization(organizationId);
        organizationService.requireMembership(organization, user);
        return ResponseEntity.ok(invitationRepository
                .findByOrganization_IdAndStatusOrderByCreatedAtDesc(organizationId,
                        OrganizationInvitationStatus.ACTIVE)
                .stream()
                .map(invitation -> OrganizationInvitationView.from(invitation, null))
                .toList());
    }

    @DeleteMapping("/{organizationId}/invitations/{invitationId}")
    @Operation(summary = "Revoke an invitation")
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID invitationId) {
        User user = currentUserResolver.requireUser(jwt);
        Organization organization = organizationService.requireOrganization(organizationId);
        organizationService.requireMembership(organization, user);

        OrganizationInvitation invitation = invitationRepository.findById(invitationId)
                .filter(candidate -> candidate.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new EntityNotFoundException("Invitation was not found: " + invitationId));
        if (!invitation.getCreatedByUserId().equals(user.getId())) {
            OrganizationMembership membership = organizationService.requireMembership(organization, user);
            if (!membership.canManageMemberships()) {
                throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                        "Only the invitation creator or an organization administrator may revoke it.");
            }
        }
        invitation.revoke();
        invitationRepository.save(invitation);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        String normalized = sort == null || sort.isBlank() ? "role" : sort.trim().toLowerCase();
        return switch (normalized) {
            case "role" -> Sort.by(Sort.Direction.ASC, "role");
            case "-role" -> Sort.by(Sort.Direction.DESC, "role");
            case "status" -> Sort.by(Sort.Direction.ASC, "status");
            case "-status" -> Sort.by(Sort.Direction.DESC, "status");
            default -> Sort.by(Sort.Direction.ASC, "role");
        };
    }

    public record CreateOrganizationRequest(
            @NotBlank(message = "is required")
            @Size(min = 2, max = 160, message = "must be between 2 and 160 characters")
            String name,

            @Size(max = 200, message = "must be at most 200 characters")
            String legalName,

            @Size(max = 500, message = "must be at most 500 characters")
            String description) {
    }

    public record JoinOrganizationRequest(
            @NotBlank(message = "is required")
            @Size(min = 8, max = 128, message = "must be between 8 and 128 characters")
            String joinCode) {
    }

    public record UpdateMembershipRequest(
            @NotNull(message = "is required")
            OrganizationMembershipRole role) {
    }

    public record CreateInvitationRequest(
            @Size(min = 8, max = 128, message = "must be between 8 and 128 characters when supplied")
            String joinCode) {
    }

}