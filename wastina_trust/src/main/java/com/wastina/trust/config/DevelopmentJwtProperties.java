package com.wastina.trust.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the local, Keycloak-free JWT authentication mode.
 *
 * <p>This mode exists purely so the frontend can be developed and integration-tested without a
 * running authorization server. It issues and validates a real, signed, HS256 bearer token
 * through Spring Security's standard {@code BearerTokenAuthenticationFilter}; it does
 * <em>not</em> bypass authentication.
 *
 * <p>{@link #isEnabled()} is deliberately guarded by {@link DevelopmentAuthenticationGuard} so
 * the mode cannot start under a production or staging profile.
 */
@Validated
@ConfigurationProperties(prefix = "wastina.dev.jwt")
public class DevelopmentJwtProperties {

    /**
     * Enables the local JWT issuer/decoder. Must only ever be true together with the
     * {@code local} or {@code development} profile.
     */
    private boolean enabled = false;

    /** Issuer claim written into issued tokens and required on incoming tokens. */
    @NotBlank
    private String issuer = "http://localhost/wastina-dev-issuer";

    /** Audience claim written into issued tokens and required on incoming tokens. */
    @NotBlank
    private String audience = "wastina-api";

    /**
     * HMAC-SHA256 signing secret. Development-only, configurable through
     * {@code WASTINA_DEV_JWT_SECRET}, and never sourced from a production secret store.
     * Must be at least 32 characters.
     */
    @NotBlank
    private String secret = "wastina-local-development-signing-secret-change-me";

    /** Access token lifetime in seconds. */
    @Positive
    private long ttlSeconds = 3600L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Duration getTtl() {
        return Duration.ofSeconds(ttlSeconds);
    }

    /** HS256 requires a key of at least 256 bits. */
    public void validateSecretStrength() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "wastina.dev.jwt.secret must be at least 32 characters to sign HS256 tokens.");
        }
    }
}