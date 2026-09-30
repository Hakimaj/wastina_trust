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
