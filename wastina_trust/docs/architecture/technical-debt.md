# Technical Debt Register

Updated after the backend remediation work. Items fixed in this pass are marked **Resolved** with
the test that covers them; the rest remain open.

| ID | Priority | Problem | Status |
|---|---|---|---|
| TD-001 | Critical | Controllers accepted actor/user IDs from request input (BOLA, impersonation) | **Resolved** — actors derived from the JWT across payments, guarantee requests, contracts, guarantees, documents, compliance, employment and trust. Covered by `OwnershipAuthorizationTest` (17 tests) and `ComplianceAuthorizationTest` (9 tests). |
| TD-002 | Critical | Outbox persistence exists but no RabbitMQ publisher | **Open** — unchanged. No frontend-facing impact. |
| TD-003 | High | Payment provider integrations are mock-only | **Open** — `MOCK` is the only implemented provider. `CHAPA`, `TELEBIRR` and `CBE_BIRR` enum values have no adapter. |
| TD-004 | High | Document storage and malware scanning are mock-only | **Open** — storage is an in-memory `ConcurrentHashMap`; documents do not survive a restart. Scanner is a substring check for `EICAR-TEST-FILE`. |
| TD-005 | High | No full integration/E2E suite or restore drill | **Partially resolved** — 125 backend tests now run against a real database and the real security filter chain (`AbstractIntegrationTest` + H2). Still no containerised E2E, no Testcontainers, no DR drill. |
| TD-006 | Medium | No centralized audit port or immutable audit store | **Open** — unchanged. |
| TD-007 | Medium | No OpenAPI artifact or generated contract tests | **Resolved** — springdoc added; `/v3/api-docs` and `/swagger-ui.html` in local, development and docker. Contract tests are hand-written, not generated. |
| TD-008 | Medium | No production IaC, managed database or secret manager | **Open** — unchanged. |

## New and remaining items

| ID | Priority | Problem | Impact |
|---|---|---|---|
| TD-009 | High | Flyway migrations are not a complete baseline; `V1` alters a `users` table no migration creates | Every runnable profile uses `ddl-auto=update` and disables Flyway, so schema is not reproducible. See `schema-management.md`. No baseline migration was authored in this pass because doing so risks data loss on an existing database. |
| TD-010 | High | `organization_invitations` exists only via Hibernate, not in any migration | Would be missing on a Flyway-managed database. Blocked by TD-009. |
| TD-011 | High | `docker-compose.yml` starts the app with `ddl-auto=update` | An application instance can alter a database schema that may be in use. Should be `validate` once TD-009 is done. |
| TD-012 | Medium | Payment webhook signature verification is mock-only (`signature.startsWith("mock-")`) | Not a real verification mechanism. Real HMAC/secret verification belongs in a provider adapter. |
| TD-013 | Medium | `AccountStatusFilter` still uses `response.sendError`, producing a non-standard body | Returns `403` with a container error body rather than the `ApiError` envelope. Other failures use the envelope, so this one is inconsistent. |
| TD-014 | Medium | Identity verification has no provider callback route | `POST /{id}/complete` is owner-driven. A real Fayda integration needs a provider-authenticated webhook, which does not exist. |
| TD-015 | Medium | `RoleController` requires `SCOPE_role:read`/`SCOPE_role:manage`, which the committed Keycloak realm does not define | Those endpoints return `403` for every caller. `ROLE_ADMIN` is the authority that actually works and is used by the new compliance endpoints. |
| TD-016 | Low | `EmploymentRecord.description` has a 2000-char column but no length validation | Truncation depends on the database. |
| TD-017 | Low | `MockIdentityVerificationProvider` decides failure from `userId.hashCode() % 5 == 0` | Deterministic, but the rule is arbitrary. Documented in the provider. |
| TD-018 | Low | `Guarantee.publicReference` is a `String`, internal id a `Long` | Addressable either way; the service accepts both. Endpoint paths remain inconsistent across domains (`{id}` vs `{publicReference}`). |
| TD-019 | Low | Neo4j is configured in `local` but `TrustEngineController` uses the JPA trust path | The graph is effectively unused. `PersonNodeRepository` is mocked in tests. |

## Test-environment notes

These are properties of the test setup, not of the production code, but are worth recording.

| ID | Note |
|---|---|
| TD-020 | H2 maps `Instant` to a zone-less `TIMESTAMP`. Surefire pins `-Duser.timezone=UTC` so instants round-trip correctly; without it, upload-session expiry compared as already expired. PostgreSQL, used in every real profile, is unaffected. |
| TD-021 | Integration tests use H2 in PostgreSQL compatibility mode, not PostgreSQL. Dialect-specific SQL would not be caught. Testcontainers would be the fix. |