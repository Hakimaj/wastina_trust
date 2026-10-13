package com.wastina.trust.controller;

import com.wastina.trust.dto.OrganizationMembershipView;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own organization memberships.
 *
 * <p>Optional {@code status} filtering was added because a client switching organizations in the
 * header must only ever be offered memberships it can actually act through.
 */
@RestController
@RequestMapping("/api/v1/me/organizations")
@Tag(name = "Organizations", description = "Current account organization memberships")
public class CurrentUserOrganizationController {

    private final CurrentUserResolver currentUserResolver;
    private final OrganizationService organizationService;

    public CurrentUserOrganizationController(CurrentUserResolver currentUserResolver,
                                             OrganizationService organizationService) {
        this.currentUserResolver = currentUserResolver;
        this.organizationService = organizationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List the caller's organization memberships")
    public java.util.List<OrganizationMembershipView> listCurrentUserOrganizations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrganizationMembershipStatus status) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        var memberships = organizationService.listMembershipsForUser(userId);
        return memberships.stream()
                .filter(membership -> status == null || membership.getStatus() == status)
                .map(OrganizationMembershipView::from)
                .toList();
    }
}