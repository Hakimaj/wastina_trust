package com.wastina.trust.dto;

import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import com.wastina.trust.entity.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One organization membership belonging to the authenticated user.
 *
 * <p>{@code canVerifyEmployment} is computed server-side from the persisted role and status so
 * the frontend never re-implements the rule that decides who may verify employment.
 */
@Schema(name = "OrganizationMembership")
public record OrganizationMembershipView(
        @Schema(description = "Membership identifier")
        UUID id,

        @Schema(description = "Organization identifier")
        UUID organizationId,

        @Schema(description = "Organization display name")
        String name,

        @Schema(description = "Organization registered legal name")
        String legalName,

        @Schema(description = "Organization verification state")
        OrganizationStatus organizationStatus,

        @Schema(description = "Membership role")
        OrganizationMembershipRole role,

        @Schema(description = "Membership state")
        OrganizationMembershipStatus membershipStatus,

        @Schema(description = "When the membership started")
        LocalDateTime joinedAt,

        @Schema(description = "When the membership ended, if it has")
        LocalDateTime endedAt,

        @Schema(description = "True when this membership may verify employment records")
        boolean canVerifyEmployment,

        @Schema(description = "True when this membership may manage other members")
        boolean canManageMemberships) {

    public static OrganizationMembershipView from(OrganizationMembership membership) {
        return new OrganizationMembershipView(
                membership.getId(),
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getOrganization().getLegalName(),
                membership.getOrganization().getStatus(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt(),
                membership.getEndedAt(),
                membership.canVerifyEmployment(),
                membership.canManageMemberships());
    }
}