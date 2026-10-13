package com.wastina.trust.dto;

import com.wastina.trust.entity.AccountStatus;
import com.wastina.trust.entity.IdentityStatus;
import com.wastina.trust.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The representation of the authenticated account returned by {@code /api/v1/me}.
 *
 * <p>The audit found that the original projection omitted {@code email} and {@code phoneNumber},
 * which made the frontend unable to display contact details and made {@code PATCH /api/v1/me}
 * impossible to confirm. Those fields are now included.
 *
 * <p>The four legacy keys ({@code id}, {@code account_status}, {@code identity_status},
 * {@code roles}) keep their original names and values so existing consumers do not break; the
 * added keys are pure additions. {@code roles} carries the persisted database roles, which is the
 * source of truth for route access and navigation.
 */
@Schema(name = "CurrentUser", description = "The authenticated Wastina account")
public record CurrentUserView(
        @Schema(description = "Account identifier")
        UUID id,

        @Schema(description = "Provider subject this account is linked to")
        String identitySubject,

        @Schema(description = "Contact email address")
        String email,

        @Schema(description = "Contact phone number")
        String phoneNumber,

        @Schema(description = "Lifecycle state of the account")
        AccountStatus accountStatus,

        @Schema(description = "Lifecycle state of identity verification")
        IdentityStatus identityStatus,

        @Schema(description = "Application roles persisted for this account, e.g. ROLE_WORKER")
        List<String> roles,

        @Schema(description = "True when the account has been deactivated by an operator")
        Boolean enabled,

        @Schema(description = "When identity verification completed, if ever")
        Instant verifiedAt,

        @Schema(description = "When the account was created")
        LocalDateTime createdAt,

        @Schema(description = "When the account was last modified")
        LocalDateTime updatedAt) {

    public static CurrentUserView from(User user) {
        return new CurrentUserView(
                user.getId(),
                user.getIdentitySubject(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAccountStatus(),
                user.getIdentityStatus(),
                user.getRoles().stream()
                        .map(role -> role.getName() == null ? null : role.getName().name())
                        .filter(java.util.Objects::nonNull)
                        .sorted()
                        .toList(),
                user.isEnabled(),
                user.getVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    /** Backward-compatible projection using the original snake_case key names. */
    public java.util.Map<String, Object> toLegacyMap() {
        java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", id);
        view.put("account_status", accountStatus);
        view.put("identity_status", identityStatus);
        view.put("roles", roles);
        return view;
    }
}