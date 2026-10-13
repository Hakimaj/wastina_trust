# Local JWT Authentication (no Keycloak required)

## What this is

A development authentication mode that issues and validates real, signed JWT bearer tokens without
requiring a running Keycloak server. It exists so the React frontend can be developed and
integration-tested locally.

**It is not an authentication bypass.** The token is signature-checked, expiry-checked,
issuer-checked and audience-checked by Spring Security's standard
`BearerTokenAuthenticationFilter` before any controller runs. Presenting no token, a malformed
token, a wrongly signed token, an expired token, a token from another issuer, or a token for another
audience all produce `401`. This is covered by `LocalJwtAuthenticationTest`.

## Personas

Four fixed synthetic accounts. The token endpoint accepts nothing else — there is no way to request
an arbitrary subject, email or authority set.

| Persona     | Subject        | Email                    | Phone           | Roles             |
|-------------|----------------|--------------------------|-----------------|-------------------|
| `worker`    | `dev-worker`   | worker@wastina.local     | +251900000001   | `ROLE_WORKER`     |
| `employer`  | `dev-employer` | employer@wastina.local   | +251900000002   | `ROLE_EMPLOYER`   |
| `guarantor` | `dev-guarantor`| guarantor@wastina.local  | +251900000003   | `ROLE_GUARANTOR`  |
| `admin`     | `dev-admin`    | admin@wastina.local      | +251900000004   | `ROLE_ADMIN`      |

Each is seeded with its account row, role and matching profile by `DevelopmentAccountSeeder`. A
development organization is seeded too: the employer persona is its `OWNER` (so it can verify
employment) and the worker persona is an `EMPLOYEE` (so it can create employment records).

## Starting the backend locally

PostgreSQL and Neo4j are required; Keycloak is not.

```powershell
# 1. Start dependencies (Postgres, Neo4j; Keycloak is optional and not needed)
cd wastina_trust
docker compose up -d postgres neo4j

# 2. Run the backend with local JWT authentication
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile enables `wastina.dev.jwt.enabled=true` by default and points at
`localhost:5432` and `bolt://localhost:7687`.

Verify it is up:

```powershell
curl http://localhost:8080/actuator/health
```

## Getting a token

```powershell
curl -X POST http://localhost:8080/api/v1/dev/auth/token `
  -H "Content-Type: application/json" `
  -d '{"persona":"worker"}'
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2026-10-12T10:00:00Z",
  "subject": "dev-worker",
  "role": "ROLE_WORKER"
}
```

To list the available personas:

```powershell
curl http://localhost:8080/api/v1/dev/auth/personas
```

## Calling an API

```powershell
curl http://localhost:8080/api/v1/me -H "Authorization: Bearer <accessToken>"
```

Store the token in memory only. Do not put it in `localStorage` or a cookie.

## Environment variables

### Local JWT mode (`local` / `development` profile)

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `WASTINA_DEV_JWT_ENABLED` | yes | `true` in `local`/`development` | Enables the local issuer and decoder |
| `WASTINA_DEV_JWT_SECRET` | recommended | local-only default in `application-local.properties` | HS256 signing key, min 32 characters |
| `WASTINA_DEV_JWT_ISSUER` | no | `http://localhost/wastina-dev-issuer` | `iss` claim; required on incoming tokens |
| `WASTINA_DEV_JWT_AUDIENCE` | no | `wastina-api` | `aud` claim; required on incoming tokens |
| `WASTINA_DEV_JWT_TTL_SECONDS` | no | `3600` | Access token lifetime |
| `WASTINA_DATABASE_URL` | yes | `jdbc:postgresql://localhost:5432/wastina_db` | PostgreSQL |
| `WASTINA_DATABASE_USERNAME` | yes | `wastina` | PostgreSQL user |
| `WASTINA_DATABASE_PASSWORD` | yes | — | PostgreSQL password |
| `SPRING_NEO4J_URI` | yes | `bolt://localhost:7687` | Neo4j for the trust graph |
| `SPRING_NEO4J_AUTHENTICATION_USERNAME` | yes | `neo4j` | Neo4j user |
| `SPRING_NEO4J_AUTHENTICATION_PASSWORD` | yes | — | Neo4j password |
| `WASTINA_CORS_ALLOWED_ORIGINS` | recommended | `http://localhost:5173` | Browser origins |
| `WASTINA_FRONTEND_ORIGIN` | no | `http://localhost:5173` | Local dev origin |

The local profile also sets
`spring.autoconfigure.exclude=...OAuth2ResourceServerAutoConfiguration` so Spring Boot does not try
to contact an OIDC issuer at startup.

### Keycloak / OIDC mode (production, `docker`)

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `WASTINA_OIDC_ISSUER` | yes | `http://localhost:8081/realms/wastina` | Issuer URI; tokens validated against its JWKS |
| `WASTINA_OIDC_AUDIENCE` | yes | `wastina-api` | Required `aud` claim |
| `WASTINA_DEV_JWT_ENABLED` | **must be false** | `false` | Leaving this on in production fails startup |
| `WASTINA_CORS_ALLOWED_ORIGINS` | **yes in production** | — | The deployed frontend origin |
| `WASTINA_SERVER_PORT` | no | `8080` | HTTP port |

Start with Keycloak:

```powershell
docker compose up -d postgres neo4j keycloak
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

## Frontend configuration

The backend listens on `WASTINA_SERVER_PORT` (default `8080`), so:

```
VITE_API_BASE_URL=http://localhost:8080
```

Note the committed `frontend/.env.local` currently points at port `8082`, which does not match the
backend. Update it to `8080`.

To keep tokens in memory and avoid Keycloak entirely, the frontend can call the dev token endpoint
and keep the result in a module-scoped variable, clearing it on logout.

## Safety rules

| Rule | Enforced by |
|---|---|
| Development auth cannot start outside `local`/`development`/`test` | `DevelopmentBypassProfileGuard` |
| Development auth cannot start alongside `staging`/`prod`/`production`/`docker` | `DevelopmentBypassProfileGuard` |
| The two security chains are mutually exclusive | `@ConditionalOnProperty` on each config |
| No development encoder/decoder exists when the mode is off | `LocalJwtModeIsolationTest` |
| The token endpoint does not exist when the mode is off | `DevelopmentAuthController` is conditional |
| Synthetic personas are never seeded when the mode is off | `DevelopmentAccountSeeder` is conditional |
| Only the four fixed personas can be requested | `DevelopmentPersona` is a closed enum |
| The signing key is at least 32 characters | `DevelopmentJwtProperties.validateSecretStrength()` |

## Production authentication

Unchanged and preserved:

- `docker/keycloak/wastina-realm.json` — realm `wastina`, client `wastina-api`, audience mapper
- `SecurityConfig` — OIDC resource server chain
- `AccountStatusFilter` — requires an `ACTIVE` database account on every request
- `CurrentUserResolver` — resolves the JWT `sub` to a `User`
- Frontend `AuthProvider` (keycloak-js, Authorization Code + PKCE), `/login`, `/auth/callback`,
  `keycloak-js` dependency

Nothing from the OIDC path was deleted. The only removed class was
`DevelopmentAuthenticationFilter`, which injected an authenticated principal for every request
without checking a token — exactly the bypass this task required replacing with real JWT
validation.

## Authorities

`JwtAuthorityConfig` maps JWT claims to Spring Security authorities identically in both chains:

| Claim | Becomes |
|---|---|
| `scope` / `scp` | `SCOPE_*` |
| `roles` (array or delimited string) | `ROLE_*` |
| `realm_access.roles` (Keycloak nesting) | `ROLE_*` |

`ROLE_ADMIN` is not self-service: `ERole.isSelfService()` is false for it, so
`POST /api/v1/me/provision` silently ignores a request for it. Only Keycloak realm roles or an
operator can grant it.

## Tests

```powershell
mvn test -Dtest=LocalJwtAuthenticationTest        # JWT validation, negative cases
mvn test -Dtest=LocalJwtModeIsolationTest         # modes are mutually exclusive
mvn test -Dtest=DevelopmentBypassProfileGuardTest # profile guard
mvn test -Dtest=CorsPreflightTest                 # browser preflight
mvn test                                           # everything
```