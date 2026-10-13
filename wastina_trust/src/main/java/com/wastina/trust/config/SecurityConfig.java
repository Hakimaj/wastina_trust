package com.wastina.trust.config;

import com.wastina.trust.security.AccountStatusFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Production / OIDC security chain.
 *
 * <p>Registers only when the local development JWT mode is <em>off</em>, so the two chains can
 * never both be active. Tokens are validated against the configured Keycloak issuer
 * ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri}); no development signing key is
 * consulted here, because {@link DevelopmentJwtConfig} does not contribute a {@code JwtDecoder}
 * in this mode.
 *
 * <p>CORS comes from the shared {@link CorsConfig} bean, so a deployed frontend is not blocked by
 * a development-only preflight policy.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "wastina.dev.jwt", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    private final AccountStatusFilter accountStatusFilter;

    public SecurityConfig(AccountStatusFilter accountStatusFilter) {
        this.accountStatusFilter = accountStatusFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // CORS preflight must never require a token.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        http.addFilterAfter(accountStatusFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}