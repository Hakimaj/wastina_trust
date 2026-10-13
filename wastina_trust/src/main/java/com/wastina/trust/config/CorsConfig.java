package com.wastina.trust.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Browser-facing CORS policy shared by every {@code SecurityFilterChain}.
 *
 * <p>This is intentionally NOT scoped to a development profile: the production chain
 * ({@link SecurityConfig}) and the local development chain ({@link DevelopmentSecurityConfig})
 * both consume this bean, so a deployed frontend is never blocked by a missing preflight
 * policy.
 *
 * <p>Origins are supplied through {@code wastina.cors.allowed-origins} so that staging and
 * production lists stay separate from the local development list. Credentials are never
 * allowed because authentication uses an {@code Authorization} bearer header rather than cookies.
 */
@Configuration
public class CorsConfig {

    /** Headers the browser is allowed to send on cross-origin API requests. */
    private static final List<String> ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Accept-Language",
            "X-Request-Id",
            "X-Correlation-Id",
            "X-Detected-Mime-Type");

    /** Response headers the browser is allowed to read. */
    private static final List<String> EXPOSED_HEADERS = List.of(
            "X-Request-Id",
            "X-Correlation-Id");

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${wastina.cors.allowed-origins:}") String configuredOrigins,
            @Value("${wastina.dev.cors.allowed-origin:}") String developmentOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveOrigins(configuredOrigins, developmentOrigin));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.HEAD.name()));
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        return new UrlBasedCorsConfigurationSource() {{
            registerCorsConfiguration("/**", configuration);
        }};
    }

    /**
     * Merges the environment-specific production/staging origins with the local development
     * origin. Blank entries are discarded so a missing property never produces a wildcard.
     */
    private List<String> resolveOrigins(String configuredOrigins, String developmentOrigin) {
        Stream<String> declared = Arrays.stream(configuredOrigins.split(","));
        List<String> origins = Stream.concat(declared, Stream.of(developmentOrigin))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        return origins.isEmpty() ? List.of("http://localhost:5173") : origins;
    }
}