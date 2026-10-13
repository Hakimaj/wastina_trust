# Wastina Architecture Decision Records

**Author:** Hakimaj (Lakim)
**Date:** 2026-09-30

## ADR-001: Modular monolith before independent microservices

- **Status:** Accepted for Task 1
- **Decision:** One Spring Boot deployment with strict bounded-context modules and explicit ports.
- **Options:** Microservices now, modular monolith, hybrid.
- **Reason:** The current repository is one application and the team benefits from simple deployment and strong local transactions. Module boundaries preserve later extraction.
- **Risks:** Shared runtime can hide coupling; enforce package rules, schema credentials, contract tests, and no cross-module repositories.
- **Revisit when:** Teams, load, availability, or security boundaries require independent deployment.

## ADR-002: PostgreSQL schema ownership

- **Status:** Accepted for target architecture
- **Decision:** PostgreSQL cluster initially, separate schema and database role per context; independent databases on extraction.
- **Reason:** Strong transactions, exact numeric money, mature backups/PITR, lower operations than many databases.
- **Risks:** Cluster failure affects contexts; use managed HA/PITR and keep extraction paths documented.

## ADR-003: OIDC authorization server boundary

- **Status:** Accepted
- **Decision:** Use Keycloak or an equivalent managed OIDC provider; Wastina remains a resource/API and business authorization system.
- **Reason:** Avoid custom identity-provider cryptography and protocol mistakes; support browser/mobile/admin clients consistently.
- **Risks:** Provider operations and configuration drift; manage realm configuration as code and test issuer/JWKS/audience behavior.

## ADR-004: Short-lived audience-restricted JWT access tokens

- **Status:** Accepted
- **Decision:** Asymmetric signed access tokens, minimal claims, 5-10 minute target, rotated refresh tokens, server-side session/revocation.
- **Reason:** Efficient resource-server validation while limiting replay and privilege scope.
- **Risks:** Revocation is not instantaneous for an access token; use short TTL and deny suspended accounts at the resource layer.

## ADR-005: RabbitMQ and transactional outbox

- **Status:** Accepted for initial scale
- **Decision:** RabbitMQ durable queues and dead-lettering with an outbox/inbox pattern.
- **Reason:** Wastina needs reliable workflow events, retries, and moderate throughput more than Kafka-scale replay.
- **Risks:** At-least-once delivery and ordering limits; consumers must deduplicate and aggregate-key ordering must be explicit.

## ADR-006: S3-compatible object storage

- **Status:** Accepted
- **Decision:** Private S3-compatible bucket abstraction, MinIO locally, signed short-lived downloads, quarantine before publication.
- **Reason:** Standard object semantics and provider portability; documents must not pass through the relational database.
- **Risks:** Provider lifecycle/region constraints; retention, legal hold, and restore tests are mandatory.

## ADR-007: KMS/Vault and purpose-separated keys

- **Status:** Accepted in principle; provider selection pending
- **Decision:** KMS/Vault-backed envelope encryption; separate encryption, JWT signing, contract signing, and webhook secrets.
- **Reason:** Central rotation, access policy, recovery, and audit.
- **Risks:** Key loss can make retained data unavailable; test break-glass recovery and retain key versions.

## ADR-008: Fayda adapter, not Fayda-shaped persistence

- **Status:** Accepted
- **Decision:** `IdentityVerificationProvider` isolates Fayda, sandbox, and mock providers. Store minimal opaque references and outcomes.
- **Reason:** Official public material does not confirm the relying-party protocol or returned artifact; the domain must not encode speculation.
- **Risks:** Partner onboarding may require a different flow; adapter contract and official conformance tests absorb the change.

## ADR-009: Authenticated field-level encryption for exceptional restricted fields

- **Status:** Accepted
- **Decision:** Do not store raw national ID by default. If approved as necessary, use envelope encryption and AES-256-GCM through vetted libraries with nonce/key IDs and access auditing.
- **Reason:** Minimization is stronger than encrypting unnecessary data; authenticated encryption protects confidentiality and integrity.
- **Risks:** Search and recovery are more complex; use blind indexes only after privacy/security review.

## ADR-010: Defer Kubernetes

- **Status:** Accepted for MVP
- **Decision:** Docker Compose locally and managed container/VM deployment first; Kubernetes requires evidence and a new ADR.
- **Reason:** Kubernetes adds operational complexity without a demonstrated need at the current repository/team scale.
- **Risks:** Coarse scaling and fewer built-in scheduling features; extraction and container health contracts keep migration feasible.

## ADR-011: Guarantee state machine remains explicit and domain-owned

- **Status:** Accepted for Task 4
- **Decision:** Guarantee lifecycle states are modeled as an explicit domain enum rather than a single generic status field; liability and involvement are separate concepts.
- **Reason:** Digital guarantees have legal, financial, and lifecycle meanings that differ from a simple CRUD flag.
- **Risks:** Requires stronger domain validation and clearer transitions; future legal changes may require a new state model.

## ADR-012: Separate payment intent state from guarantee state

- **Status:** Accepted for Task 4
- **Decision:** Payment state is kept distinct from guarantee state, because a provider may succeed while a guarantee remains pending or active depending on business rules.
- **Reason:** Payment success, webhook validation, and guarantee state transitions must not collapse into the same boolean or enum.
- **Risks:** The integration requires stronger orchestration and reconciliation logic; a simple "paid" field is insufficient.

## ADR-013: Canonical contract representation before hashing

- **Status:** Accepted for Task 4
- **Decision:** A canonical, deterministic contract serialization is created before hashing so tampering is visible and historical versions remain verifiable.
- **Reason:** A raw JSON dump is not equivalent to a canonical representation; a stable serialization is required for integrity checks.
- **Risks:** Canonicalization must stay consistent across versions; any future format change requires a new contract version and ADR review.

## ADR-014: Provider abstraction before production payment integration

- **Status:** Accepted for Task 4
- **Decision:** Payment providers are abstracted behind a provider interface and a mock adapter until the official API contract is confirmed.
- **Reason:** Chapa, Telebirr, and CBE Birr have different auth, callback, and verification behavior; the domain should not hard-code provider assumptions.
- **Risks:** Real provider contracts may differ from the mock flow; onboarding and signature verification remain human review items.

## ADR-015: Human legal review required for guarantee enforceability and payment regulation

- **Status:** Accepted for Task 4
- **Decision:** The implementation clearly distinguishes legal/electronic-signature requirements from technical hash/signature implementation and marks compliance decisions as manual review items.
- **Reason:** A SHA-256 hash is not the same as a legally recognized electronic signature, and payment activity may trigger additional regulator obligations.
- **Risks:** Full legal and provider onboarding work is still required before launch; do not claim compliance without review.

## ADR-016: Organization vs employer account separation

- **Status:** Accepted for Task 5
- **Decision:** Organization authority is scoped to a legal/business entity, not a generic user role.
- **Reason:** A user can be an employee or HR representative across multiple organizations without being automatically authorized to verify all employment claims.
- **Risks:** Requires good membership and permission checks; historical attribution must remain intact after offboarding.

## ADR-017: Employment record model preserves claim + verification + resolution

- **Status:** Accepted for Task 5
- **Decision:** The system keeps the original claim, employer assertion, and resolved state distinct.
- **Reason:** Employment history is sensitive and potentially disputed; silent overwrites would destroy audit value.
- **Risks:** Requires correction and revocation flows that are more explicit than normal CRUD updates.

## ADR-018: Verification state machine is explicit and field-aware

- **Status:** Accepted for Task 5
- **Decision:** Verification uses explicit lifecycle states and supports partial verification of specific fields.
- **Reason:** Employment claims can be partly correct, disputed, or corrected without destroying the original record.
- **Risks:** Needs careful lifecycle validation and event emission semantics.

## ADR-019: Occupation classification is reference-data, not a forced legal identity

- **Status:** Accepted for Task 5
- **Decision:** The system supports bilingual job titles plus optional standardized occupation codes and local terms.
- **Reason:** Local Ethiopian job meaning is not always equivalent to a foreign classification schema.
- **Risks:** Historical occupation interpretation depends on version tracking and disciplined reference-data import.

## ADR-022: Separate Financial and Document Gateways

- **Status:** Accepted for Task 7
- **Decision:** Payment and Document are independent infrastructure bounded contexts with replaceable provider/storage ports.
- **Reason:** Payment provider details and object-storage/scanning details must not leak into Guarantee, Compliance, Employment, or IAM.
- **Risks:** Requires explicit cross-context contracts, audit, outbox delivery, and provider/storage operational ownership.

## ADR-023: Quarantine uploads and short-lived document access

- **Status:** Accepted for Task 7
- **Decision:** Uploads enter private quarantine, pass scanning/validation, then become available; downloads use authorization-first short-lived signed URLs.
- **Reason:** Uploaded bytes are not automatically safe or authorized, and signed URLs are bearer access.
- **Risks:** Scanner, KMS, object storage, URL leakage, retention, and restore operations require production controls and testing.

## ADR-024: Payment idempotency, verification, and reconciliation

- **Status:** Accepted for Task 7
- **Decision:** Intent idempotency, provider transaction identity, webhook deduplication, server verification, refund limits, and reconciliation are separate controls.
- **Reason:** Provider callbacks and network timeouts are ambiguous; amount/currency/reference mismatches must not silently fulfill business state.
- **Risks:** Provider-specific contracts and Ethiopian payment regulation remain human review requirements.

## ADR-025: Task 8 operational baseline remains a modular monolith

- **Status:** Accepted for Task 8
- **Decision:** Keep the Spring Boot modular monolith and PostgreSQL migration stream; add outbox persistence, Actuator probes, local Compose dependencies, a non-root container, and CI artifact checks. Kubernetes remains deferred.
- **Reason:** The repository has one application and no evidence yet requiring independent deployment or scaling.
- **Risks:** Production still requires broker publishers/consumers, managed infrastructure, audit, security testing, and DR exercises.

## ADR-026: Health and observability baseline

- **Status:** Accepted for Task 8
- **Decision:** Use Spring Boot Actuator for safe health/readiness/metrics exposure, with detailed health protected by authorization and future OpenTelemetry-compatible tracing.
- **Reason:** The application needs deployable health contracts without exposing sensitive domain data.
- **Risks:** Dashboards, alerts, trace backend, ownership, and incident exercises remain deployment-specific follow-up work.
