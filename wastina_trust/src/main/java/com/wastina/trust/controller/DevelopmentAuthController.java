package com.wastina.trust.controller;

import com.wastina.trust.config.DevelopmentJwtIssuer;
import com.wastina.trust.config.DevelopmentPersona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local development authentication entry point.
 *
 * <p>This controller exists only when {@code wastina.dev.jwt.enabled=true}, which in turn is
 * impossible under a production or staging profile (see {@code DevelopmentAuthenticationGuard}).
 * It hands out a signed bearer token for one of the fixed synthetic personas so the frontend can
 * be developed without a running Keycloak server.
 *
 * <p>The resulting token is used exactly like a Keycloak token:
 * {@code Authorization: Bearer <access_token>}. It is verified by the standard
 * {@code BearerTokenAuthenticationFilter} against the development {@code JwtDecoder}, and it is
 * additionally subject to the normal {@code AccountStatusFilter} check against the seeded
 * database account.
 */
@RestController
@RequestMapping(path = "/api/v1/dev/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "wastina.dev.jwt", name = "enabled", havingValue = "true")
public class DevelopmentAuthController {

    private final DevelopmentJwtIssuer issuer;

    public DevelopmentAuthController(DevelopmentJwtIssuer issuer) {
        this.issuer = issuer;
    }

    /** Lists the personas a developer may log in as. */
    @GetMapping("/personas")
    public List<Map<String, String>> personas() {
        return Arrays.stream(DevelopmentPersona.values())
                .map(persona -> Map.of(
                        "persona", persona.name().toLowerCase(),
                        "subject", persona.getSubject(),
                        "email", persona.getEmail(),
                        "role", persona.getRole()))
                .toList();
    }

    /**
     * Issues a signed development access token.
     *
     * <p>{@code persona} is matched against the closed {@link DevelopmentPersona} enum; unknown
     * values produce a {@code 400}. There is deliberately no way to request an arbitrary subject,
     * email or role.
     */
    @PostMapping("/token")
    public ResponseEntity<DevelopmentJwtIssuer.IssuedToken> token(
            @Valid @RequestBody TokenRequest request) {
        DevelopmentPersona persona = DevelopmentPersona.from(request.persona());
        return ResponseEntity.ok(issuer.issue(persona));
    }

    public record TokenRequest(
            @NotBlank(message = "is required")
            @Size(max = 32, message = "must be at most 32 characters")
            String persona) {
    }
}