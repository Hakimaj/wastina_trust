package com.wastina.trust.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Mints signed access tokens for the {@link DevelopmentPersona} synthetic accounts.
 *
 * <p>This service only exists when the local development JWT mode is enabled, and it can only
 * mint tokens for the fixed personas declared in {@link DevelopmentPersona}. It performs no
 * authentication of its own and is never registered in a non-development build.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "wastina.dev.jwt", name = "enabled", havingValue = "true")
public class DevelopmentJwtIssuer {

    private final JwtEncoder jwtEncoder;
    private final DevelopmentJwtProperties properties;

    public DevelopmentJwtIssuer(JwtEncoder jwtEncoder, DevelopmentJwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /** Mints a signed, expiring bearer token for the given synthetic persona. */
    public IssuedToken issue(DevelopmentPersona persona) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(persona.getSubject())
                .audience(List.of(properties.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", persona.getEmail())
                .claim("preferred_username", persona.getSubject())
                .claim("name", "Wastina " + persona.name().toLowerCase())
                .claim("roles", List.of(persona.getRole()))
                .claim("persona", persona.name().toLowerCase())
                .claim("dev_only", true);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()));

        return new IssuedToken(jwt.getTokenValue(), "Bearer", properties.getTtlSeconds(), expiresAt,
                persona.getSubject(), persona.getRole());
    }

    public record IssuedToken(
            String accessToken,
            String tokenType,
            long expiresIn,
            Instant expiresAt,
            String subject,
            String role) {
    }
}