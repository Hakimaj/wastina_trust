package com.wastina.trust.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the rule that development authentication can never run in a deployed environment.
 *
 * <p>This is the control that stops a development signing key from being accepted in production, so
 * the negative cases matter more than the positive one.
 */
class DevelopmentBypassProfileGuardTest {

    @Test
    @DisplayName("local JWT mode is permitted with the local profile")
    void allowsDevJwtWithLocalProfile() {
        MockEnvironment environment = environment("local");

        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("local JWT mode is permitted with the development and test profiles")
    void allowsDevJwtWithDevelopmentAndTestProfiles() {
        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(environment("development"))
                .run(new DefaultApplicationArguments(new String[0])));
        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(environment("test"))
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("development authentication is rejected in production")
    void rejectsDevJwtInProduction() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new DevelopmentBypassProfileGuard(environment("production"))
                        .run(new DefaultApplicationArguments(new String[0])));
        assertTrue(failure.getMessage().contains("Keycloak"),
                "the failure should point the operator at the OIDC path");
    }

    @Test
    @DisplayName("development authentication is rejected in staging and prod")
    void rejectsDevJwtInStagingAndProd() {
        assertThrows(IllegalStateException.class, () -> new DevelopmentBypassProfileGuard(environment("staging"))
                .run(new DefaultApplicationArguments(new String[0])));
        assertThrows(IllegalStateException.class, () -> new DevelopmentBypassProfileGuard(environment("prod"))
                .run(new DefaultApplicationArguments(new String[0])));
        assertThrows(IllegalStateException.class, () -> new DevelopmentBypassProfileGuard(environment("docker"))
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("a production profile wins even when a development profile is also active")
    void rejectsDevJwtWhenMixedWithProduction() {
        assertThrows(IllegalStateException.class,
                () -> new DevelopmentBypassProfileGuard(environment("local", "production"))
                        .run(new DefaultApplicationArguments(new String[0])));
        assertThrows(IllegalStateException.class,
                () -> new DevelopmentBypassProfileGuard(environment("development", "staging"))
                        .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("an environment with no profile is rejected")
    void rejectsDevJwtWithoutAnyProfile() {
        assertThrows(IllegalStateException.class, () -> new DevelopmentBypassProfileGuard(environment("none"))
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("the legacy auth-bypass switch is guarded by the same rule")
    void guardsLegacyAuthBypassSwitch() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("wastina.dev.auth-bypass", "true");
        environment.setActiveProfiles("local");
        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));

        MockEnvironment production = new MockEnvironment();
        production.setProperty("wastina.dev.auth-bypass", "true");
        production.setActiveProfiles("production");
        assertThrows(IllegalStateException.class, () -> new DevelopmentBypassProfileGuard(production)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("with both switches off the guard does nothing in any profile")
    void doesNothingWhenDevelopmentAuthenticationIsDisabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("wastina.dev.jwt.enabled", "false");
        environment.setProperty("wastina.dev.auth-bypass", "false");
        environment.setActiveProfiles("production");

        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    @DisplayName("an empty environment, as used in production, starts the guard without incident")
    void doesNothingWithNoPropertiesAtAll() {
        assertDoesNotThrow(() -> new DevelopmentBypassProfileGuard(new MockEnvironment())
                .run(new DefaultApplicationArguments(new String[0])));
    }

    private MockEnvironment environment(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("wastina.dev.jwt.enabled", "true");
        environment.setActiveProfiles(profiles);
        return environment;
    }
}