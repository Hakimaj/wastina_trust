package com.wastina.trust.entity;

/**
 * The application roles an account may hold.
 *
 * <p>{@link #isSelfService()} marks the roles a user may select for themselves when an account is
 * provisioned. Privileged roles such as {@link #ROLE_ADMIN} are deliberately excluded: assigning
 * them would let any new sign-up escalate itself, so only a trusted authorization server (Keycloak
 * realm role) or an operator may grant them.
 */
public enum ERole {

    ROLE_WORKER(true),
    ROLE_GUARANTOR(true),
    ROLE_EMPLOYER(true),
    ROLE_ADMIN(false);

    private final boolean selfService;

    ERole(boolean selfService) {
        this.selfService = selfService;
    }

    public boolean isSelfService() {
        return selfService;
    }

    public static boolean isSelfService(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return valueOf(normalized).selfService;
        } catch (IllegalArgumentException notARole) {
            return false;
        }
    }
}