package com.wastina.trust.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the two authentication modes are genuinely exclusive, so a deployment can never
 * end up accepting a development-signed token alongside the real OIDC issuer.
 *
 * <p>Runs under the {@code local} profile — which supplies the mock storage and payment adapters —
 * but with {@code wastina.dev.jwt.enabled=false}, i.e. the production authentication configuration.
 * Everything gated on the development JWT flag must be absent.
 */
@SpringBootTest(properties = {
        // The production authentication configuration: OIDC-backed, development mode off.
        "wastina.dev.jwt.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/does-not-resolve",
        "spring.datasource.url=jdbc:h2:mem:wastina_isolation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "springdoc.api-docs.enabled=false",
        "spring.neo4j.uri=bolt://localhost:9"
})
class LocalJwtModeIsolationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("no development JWT encoder or decoder exists when the mode is off")
    void developmentJwtBeansAreAbsent() {
        assertThat(context.getBeanNamesForType(JwtEncoder.class))
                .as("no development JwtEncoder may be registered outside local development")
                .doesNotContain("developmentJwtEncoder");

        assertThat(context.getBeanNamesForType(JwtDecoder.class))
                .as("no development JwtDecoder may be registered outside local development")
                .doesNotContain("developmentJwtDecoder");
    }

    @Test
    @DisplayName("the development token endpoint is not mapped at all")
    void developmentEndpointsAreAbsent() {
        assertThat(context.containsBean("developmentAuthController"))
                .as("the local token endpoint must not exist when the mode is off")
                .isFalse();
    }

    @Test
    @DisplayName("the development account seeder does not run, so no synthetic personas exist")
    void developmentSeederIsAbsent() {
        assertThat(context.containsBean("developmentAccountSeeder"))
                .as("the synthetic personas must never be seeded outside local development")
                .isFalse();
    }

    @Test
    @DisplayName("the production security chain is the one that is registered")
    void productionChainIsActive() {
        assertThat(context.getBeanNamesForType(SecurityFilterChain.class))
                .as("with the development mode off, only the production chain may be registered")
                .containsExactly("filterChain");
        assertThat(context.containsBean("developmentSecurityFilterChain"))
                .as("the development chain must not be registered when the mode is off")
                .isFalse();
    }
}