package com.wastina.trust.config;

/**
 * The synthetic local personas that the development token endpoint is allowed to mint tokens
 * for.
 *
 * <p>This is a closed enum on purpose. The development token endpoint accepts nothing but
 * these values, so a caller can never ask for an arbitrary subject or an arbitrary authority
 * set — in development or anywhere else. Each persona maps to a stable synthetic
 * {@code identitySubject} that {@link DevelopmentAccountSeeder} provisions in the database.
 */
public enum DevelopmentPersona {

    /** A worker: can manage their own profile and employment history. */
    WORKER("dev-worker", "worker@wastina.local", "+251900000001", "ROLE_WORKER"),

    /** An employer/verifier: holds OWNER membership so they can verify employment. */
    EMPLOYER("dev-employer", "employer@wastina.local", "+251900000002", "ROLE_EMPLOYER"),

    /** A guarantor: manages a guarantor profile. */
    GUARANTOR("dev-guarantor", "guarantor@wastina.local", "+251900000003", "ROLE_GUARANTOR"),

    /** An administrator. */
    ADMIN("dev-admin", "admin@wastina.local", "+251900000004", "ROLE_ADMIN");

    private final String subject;
    private final String email;
    private final String phoneNumber;
    private final String role;

    DevelopmentPersona(String subject, String email, String phoneNumber, String role) {
        this.subject = subject;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRole() {
        return role;
    }

    /** Case-insensitive lookup that throws a 400-friendly message for unknown values. */
    public static DevelopmentPersona from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "A development persona is required. Supported personas: " + supported() + ".");
        }
        for (DevelopmentPersona persona : values()) {
            if (persona.name().equalsIgnoreCase(value.trim())) {
                return persona;
            }
        }
        throw new IllegalArgumentException(
                "Unknown development persona '" + value + "'. Supported personas: " + supported() + ".");
    }

    public static String supported() {
        StringBuilder builder = new StringBuilder();
        for (DevelopmentPersona persona : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(persona.name().toLowerCase());
        }
        return builder.toString();
    }
}