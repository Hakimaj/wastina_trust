package com.wastina.trust.config;

import com.wastina.trust.security.AccountStatusFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Local development security chain that authenticates real, signed bearer tokens without
 * requiring a running Keycloak server.
 *
 * <p>This is a full JWT resource-server chain: it uses the ordinary
 * {@code BearerTokenAuthenticationFilter} against the development {@link JwtDecoder}
 * ({@link DevelopmentJwtConfig}). It deliberately does <em>not</em> inject an authentication into
 * the security context for unauthenticated callers — presenting no token, a malformed token, a
 * token signed with the wrong key, or an expired token all yield {@code 401}.
 *
 * <p>The only anonymous endpoints are {@code POST /api/v1/dev/auth/token} and
 * {@code GET /api/v1/dev/auth/personas}, which deal exclusively with the fixed synthetic
 * {@link DevelopmentPersona}s. Everything else requires authentication, and
 * {@link AccountStatusFilter} additionally requires a provisioned, {@code ACTIVE} database
 * account.
 *
 * <p>This chain is registered only when {@code wastina.dev.jwt.enabled=true};
 * {@code DevelopmentBypassProfileGuard} fails startup if that flag is set outside the
 * {@code local}/{@code development} profiles or together with a production-like profile.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "wastina.dev.jwt", name = "enabled", havingValue = "true")
public class DevelopmentSecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    @Bean
    public SecurityFilterChain developmentSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder developmentJwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AccountStatusFilter accountStatusFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // The only anonymous API surface: mint a token for a fixed synthetic persona.
                        .requestMatchers(HttpMethod.POST, "/api/v1/dev/auth/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/dev/auth/personas").permitAll()
                        // CORS preflight must never require a token.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(developmentJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        http.addFilterAfter(accountStatusFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}