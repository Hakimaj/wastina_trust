package com.wastina.trust.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Wires a symmetric-key JWT encoder/decoder for the local development authentication mode.
 *
 * <p>Both beans are produced only when {@code wastina.dev.jwt.enabled=true}. Because the decoder
 * is a regular {@link JwtDecoder}, the development chain uses the exact same
 * {@code BearerTokenAuthenticationFilter} code path as the Keycloak/OIDC chain: the token is
 * signature-checked, expiry-checked, issuer-checked and audience-checked before any controller
 * runs. Nothing is injected into the security context without a verified token.
 */
@Configuration
@EnableConfigurationProperties(DevelopmentJwtProperties.class)
@ConditionalOnProperty(prefix = "wastina.dev.jwt", name = "enabled", havingValue = "true")
public class DevelopmentJwtConfig {

    /**
     * Validates issuer and audience on top of the standard expiry/not-before checks, so a token
     * minted for another environment cannot be replayed against this one.
     */
    @Bean
    public JwtDecoder developmentJwtDecoder(DevelopmentJwtProperties properties) {
        properties.validateSecretStrength();
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience() != null && token.getAudience().contains(properties.getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "The token audience does not match this API.", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.getIssuer()), audienceValidator));
        return decoder;
    }

    @Bean
    public JwtEncoder developmentJwtEncoder(DevelopmentJwtProperties properties) {
        properties.validateSecretStrength();
        ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(secretKey(properties));
        return new NimbusJwtEncoder(secret);
    }

    private SecretKey secretKey(DevelopmentJwtProperties properties) {
        return new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}