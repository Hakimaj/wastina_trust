package com.wastina.trust.dto;

import com.wastina.trust.entity.OrganizationInvitation;
import com.wastina.trust.entity.OrganizationInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * An organization invitation.
 *
 * <p>{@code joinCode} is populated only in the immediate response to
 * {@code POST /api/v1/organizations/{id}/invitations}. It is never recoverable afterwards because
 * only its SHA-256 digest is persisted, so listings return {@code joinCode = null}.
 */
@Schema(name = "OrganizationInvitation")
public record OrganizationInvitationView(
        @Schema(description = "Invitation identifier")
        UUID id,

        @Schema(description = "Organization the invitation grants access to")
        UUID organizationId,

        @Schema(description = "Membership role granted on redemption")
        String grantedMembershipRole,

        @Schema(description = "Invitation state")
        OrganizationInvitationStatus status,

        @Schema(description = "When the invitation was created")
        Instant createdAt,

        @Schema(description = "When the invitation stops being redeemable")
        Instant expiresAt,

        @Schema(description = "True when the invitation can still be redeemed")
        boolean redeemable,

        @Schema(description = "Single-use join code. Returned only at creation time.")
        String joinCode) {

    public static OrganizationInvitationView from(OrganizationInvitation invitation, String oneTimeCode) {
        return new OrganizationInvitationView(
                invitation.getId(),
                invitation.getOrganization().getId(),
                invitation.getGrantedMembershipRole().name(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.isRedeemable(),
                oneTimeCode);
    }
}