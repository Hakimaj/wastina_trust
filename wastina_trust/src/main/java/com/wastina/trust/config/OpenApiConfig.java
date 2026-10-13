package com.wastina.trust.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI description served at {@code /v3/api-docs}, with Swagger UI at {@code /swagger-ui.html}.
 *
 * <p>The audit found no API documentation existed (TD-007), which left the frontend unable to
 * generate types or verify a contract. The bearer scheme below means the UI can exercise the local
 * development flow: mint a token from {@code POST /api/v1/dev/auth/token} and paste it in.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI wastinaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wastina Digital Trust Platform API")
                        .version("v1")
                        .description("""
                                Backend for the Wastina digital trust protocol: identity, worker, employer \
                                and guarantor profiles, employment verification, guarantees, contracts, \
                                documents, payments and compliance.

                                ### Authentication
                                Send `Authorization: Bearer <access_token>` on every request.

                                Two token sources are supported, selected by configuration:
                                - **Keycloak / OIDC** (production and docker): the token is issued by the \
                                realm configured in `WASTINA_OIDC_ISSUER`.
                                - **Local signed JWT** (`local` and `development` profiles only): obtain one \
                                from `POST /api/v1/dev/auth/token` with a body of \
                                `{"persona":"worker|employer|guarantor|admin"}`. Startup fails if this \
                                mode is enabled under a production profile.

                                ### Errors
                                Every failure returns a single envelope with a stable, machine-readable \
                                `code`, a `message`, the `path`, a `requestId` echoed from the \
                                `X-Request-Id` header, and `fieldErrors` for validation failures.
                                """)
                        .contact(new Contact().name("Wastina Platform Team"))
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("OIDC access token from Keycloak, or a local development token.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}