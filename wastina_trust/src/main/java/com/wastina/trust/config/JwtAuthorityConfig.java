package com.wastina.trust.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Single source of truth for turning a validated JWT into Spring Security authorities.
 *
 * <p>Both the Keycloak/OIDC chain and the local development chain use this converter, so a
 * {@code ROLE_WORKER} authority means the same thing regardless of which authorization server
 * signed the token.
 *
 * <p>Supported claim shapes:
 * <ul>
 *   <li>{@code scope} / {@code scp} — space separated, mapped to {@code SCOPE_*} authorities
 *       (Keycloak default).</li>
 *   <li>{@code roles} — a JSON array or a space/comma separated string, mapped to
 *       {@code ROLE_*} authorities. This is what the local development issuer writes.</li>
 *   <li>{@code realm_access.roles} — the standard Keycloak realm-role nesting, also mapped to
 *       {@code ROLE_*}. This makes Keycloak realm roles usable for {@code @PreAuthorize}
 *       without a custom protocol mapper.</li>
 * </ul>
 *
 * <p>Note: the authorities converter is exposed as a concrete named type rather than a lambda
 * bean. A {@code Converter} bean is picked up by Spring MVC's {@code FormattingConversionService},
 * and a generic lambda there fails to start the application.
 */
@Configuration
public class JwtAuthorityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new WastinaAuthoritiesConverter());
        return converter;
    }

    /** Concrete named type so it is never auto-registered as an MVC conversion service. */
    public static final class WastinaAuthoritiesConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }
            authorities.addAll(extractRoleAuthorities(jwt));
            return authorities;
        }

        private List<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();

            for (String role : readStringList(jwt.getClaim("roles"))) {
                addRole(authorities, role);
            }

            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> realmAccessMap) {
                for (String role : readStringList(realmAccessMap.get("roles"))) {
                    addRole(authorities, role);
                }
            }

            return authorities;
        }

        private void addRole(List<GrantedAuthority> authorities, String role) {
            if (role == null || role.isBlank()) {
                return;
            }
            String normalized = role.trim().toUpperCase(Locale.ROOT);
            String authority = normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
            authorities.add(new SimpleGrantedAuthority(authority));
        }

        /** Accepts a JSON array, a single string, or a delimited string. */
        private List<String> readStringList(Object claim) {
            if (claim instanceof Collection<?> collection) {
                return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            }
            if (claim instanceof String text && !text.isBlank()) {
                return List.of(text.split("[\\s,]+"));
            }
            return List.of();
        }
    }
}