# Task 2: IAM Implementation Handoff

**Author:** Hakimaj (Lakim)
**Status:** Initial IAM foundation implemented in `src/main/java/com/wastina/trust`

## Repository audit

The repository has one canonical Java source tree: `src/main/java/com/wastina/trust`. No parallel IAM package was created. The live Neo4j graph classes were retained because `TrustEngineService` still references them; they are not IAM code and must be migrated only under a later Trust ADR.

The removed authentication path was unsafe and contradicted Task 1: a random in-process HS256 key was generated on every start, tokens lived for 24 hours, local passwords were stored in the business entity, `/api/users` allowed public creation and unrestricted user mutation, and `application.properties` contained committed credentials. The project now uses Spring OAuth2 resource-server JWT validation against configured OIDC issuer/audience values.

## Implemented

- OIDC resource-server validation through `spring-boot-starter-oauth2-resource-server`.
- Environment-based PostgreSQL and OIDC configuration.
- `User.identitySubject` as the stable external OIDC `sub` mapping.
- Separate `AccountStatus` and `IdentityStatus` enums.
- `Consent` and `IdentityVerification` entities with provider reference, status, assurance, and optimistic version.
- `IdentityVerificationProvider` abstraction.
- Local/test mock provider; production profile fails closed until official Fayda onboarding/configuration exists.
- Consent-first identity verification orchestration and user-scoped status endpoint.
- Authenticated `/api/v1/me` read/update endpoint with server-owned status and roles.
- Flyway migration for IAM columns, consent, and verification tables.
- Legacy login, JWT issuance/filtering, local UserDetails, public user CRUD, and password/Fayda fields removed.
- Role management now requires `role:manage` or `role:read` scopes.

## Fayda status

- **IMPLEMENTED:** provider boundary, consent record, pending/success/failure state model, safe provider reference storage, mock provider.
- **MOCKED:** local `mock-*` provider references and deterministic success behavior.
- **CONFIGURATION REQUIRED:** OIDC issuer/audience and production provider credentials.
- **OFFICIAL PROVIDER ACCESS REQUIRED:** real Fayda e-KYC flow, callback/authentication format, returned subject/assurance, sandbox, limits, and production approval.
- **HUMAN REVIEW REQUIRED:** national-ID processing, consent notice, retention, deletion, cross-border processing, and controller/processor obligations under Ethiopian law.

No Fayda API, FAN/FIN field, raw national-ID value, biometric field, provider token, or provider response has been invented or stored.

## IAM contract for future modules

Later modules authenticate requests as resource servers and receive:

| Field | Meaning | Allowed consumers |
|---|---|---|
| `sub` / `identitySubject` | OIDC subject mapped to one Wastina user | IAM and authorization policy |
| internal `userId` | Wastina UUID | owning business modules and event payloads |
| scopes | permitted API actions | resource endpoint authorization |
| coarse roles | UI/policy hints only | policy evaluation, never ownership proof |
| `accountStatus` | account lifecycle gate | every protected module |
| `identityStatus` | identity-proofing outcome | only policy-relevant business decisions |

Trust, Guarantee, Employment, Dispute, Payment, and Document modules MUST NOT access password hashes, MFA secrets, refresh tokens, raw national-ID data, or provider credentials. Object-level authorization remains the owning module's responsibility: `sub + action + resource + relationship + context`.

## Known limitations

- A real Keycloak realm/client configuration has not been committed; local OIDC infrastructure still needs to be added.
- Outbox/audit persistence, session listing/revocation, password recovery, MFA, rate limiting, and comprehensive tests remain follow-up IAM work.
- Account-state transition service, session listing/revocation, password recovery, MFA, rate limiting, event publication, and comprehensive tests remain follow-up IAM work.
- Existing legacy profile/business controllers require the same object-level authorization treatment before production use.
- Maven clean validation is currently blocked because `target/classes` is locked by the local Java/editor process; source diagnostics are clean apart from inherited repository nullness warnings.

## Security requirements matrix

| ID | Requirement | Implementation | Test status | Severity |
|---|---|---|---|---|
| IAM-SEC-001 | APIs validate issuer, audience, signature, and expiry | Spring resource server + OIDC properties | Build blocked; diagnostics clean | Critical |
| IAM-SEC-002 | No local JWT signing key or password path | Legacy JWT/password classes removed | Source scan required | Critical |
| IAM-SEC-003 | User identity is mapped by OIDC subject | `identitySubject` unique field/repository lookup | Pending integration test | Critical |
| IAM-SEC-004 | Identity verification requires recorded consent | `Consent` created before provider start | Pending service test | High |
| IAM-SEC-005 | Provider outage fails closed | unavailable production provider | Pending service test | Critical |
| IAM-SEC-006 | Verification records are user-scoped | controller lookup filters by user ID | Pending IDOR test | Critical |
| IAM-SEC-007 | Role management requires explicit scope | `@PreAuthorize` on role endpoints | Pending security test | High |
| IAM-SEC-008 | Raw national ID is not stored or emitted | provider reference only | Source review | Critical |

## Human review required

Security must approve OIDC realm configuration, issuer/audience, scopes, role mapping, admin MFA, key rotation, and production network policy. Legal counsel must approve Fayda processing, privacy notice/consent, retention/deletion, cross-border processors, and breach procedures. Product must approve which workflows require `VERIFIED` identity and which roles can be assigned after verification.
