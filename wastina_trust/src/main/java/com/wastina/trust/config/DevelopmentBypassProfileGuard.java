package com.wastina.trust.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup when a development-only authentication mode is combined with an
 * environment that must never see development credentials.
 *
 * <p>Two switches are covered:
 * <ul>
 *   <li>{@code wastina.dev.auth-bypass} — the legacy request filter that injected an
 *       authenticated principal. Retained only so an old local setup keeps working, and blocked
 *       outside {@code local}/{@code development}/{@code test}.</li>
 *   <li>{@code wastina.dev.jwt.enabled} — the local signed-JWT mode. Blocked outside
 *       {@code local}/{@code development}/{@code test} and additionally blocked when a
 *       production-like profile is active at the same time.</li>
 * </ul>
 */
@Component
public class DevelopmentBypassProfileGuard implements ApplicationRunner {

    /** Profiles in which development authentication is permitted. Never includes a deployed environment. */
    private static final Set<String> ALLOWED_PROFILES = Set.of("local", "development", "test");

    /** Profiles that must never run with any development authentication mode. */
    private static final Set<String> FORBIDDEN_PROFILES = Set.of("prod", "production", "staging", "docker");
    private static final String JWT_ENABLED_PROPERTY = "wastina.dev.jwt.enabled";
    private static final String BYPASS_ENABLED_PROPERTY = "wastina.dev.auth-bypass";

    private final Environment environment;

    public DevelopmentBypassProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isEnabled(BYPASS_ENABLED_PROPERTY)) {
            requireIsolatedDevelopmentProfile(
                    "wastina.dev.auth-bypass=true",
                    "It injects an authenticated principal without validating a bearer token.");
        }
        if (isEnabled(JWT_ENABLED_PROPERTY)) {
            requireIsolatedDevelopmentProfile(
                    JWT_ENABLED_PROPERTY + "=true",
                    "It issues and accepts access tokens signed with a development-only key.");
        }
    }

    private boolean isEnabled(String property) {
        return environment.getProperty(property, Boolean.class, false);
    }

    private void requireIsolatedDevelopmentProfile(String flag, String rationale) {
        Set<String> activeProfiles = Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        boolean allowedProfileActive = activeProfiles.stream().anyMatch(ALLOWED_PROFILES::contains);
        boolean forbiddenProfileActive = activeProfiles.stream().anyMatch(FORBIDDEN_PROFILES::contains);

        if (!allowedProfileActive || forbiddenProfileActive) {
            throw new IllegalStateException(
                    flag + " is only permitted with the 'local', 'development' or 'test' profile, and "
                            + "never together with 'staging', 'prod', 'production' or 'docker'. "
                            + "Reason: " + rationale
                            + " In a non-development environment the Keycloak/OIDC issuer must be used "
                            + "instead.");
        }
    }
}