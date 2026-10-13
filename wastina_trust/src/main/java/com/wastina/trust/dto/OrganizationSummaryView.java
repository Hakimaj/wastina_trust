package com.wastina.trust.dto;

import com.wastina.trust.entity.Organization;
import com.wastina.trust.entity.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organization summary returned by the organization endpoints.
 *
 * <p>{@code canManageMemberships} and {@code isOwner} are derived per-caller on the server so the
 * frontend does not have to re-implement the role rules to decide which controls to show.
 */
@Schema(name = "OrganizationSummary")
public record OrganizationSummaryView(
        @Schema(description = "Organization identifier")
        UUID id,

        @Schema(description = "Display name")
        String name,

        @Schema(description = "Registered legal name")
        String legalName,

        @Schema(description = "Free-text description")
        String description,

        @Schema(description = "Verification state of the organization")
        OrganizationStatus status,

        @Schema(description = "When the organization was created")
        LocalDateTime createdAt,

        @Schema(description = "True when the calling user may manage memberships")
        boolean canManageMemberships,

        @Schema(description = "True when the calling user owns the organization")
        boolean isOwner) {

    public static OrganizationSummaryView from(Organization organization,
                                                boolean canManageMemberships,
                                                boolean isOwner) {
        return new OrganizationSummaryView(
                organization.getId(),
                organization.getName(),
                organization.getLegalName(),
                organization.getDescription(),
                organization.getStatus(),
                organization.getCreatedAt(),
                canManageMemberships,
                isOwner);
    }
}