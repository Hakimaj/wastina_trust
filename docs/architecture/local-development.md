# Wastina Local Development Contract

**Author:** Hakimaj (Lakim)

Task 1 defines the following local dependency contract. The implementation may choose the exact Compose filenames, but it must preserve the boundaries and must not require production secrets.

| Dependency | Local implementation | Purpose |
|---|---|---|
| Relational system of record | PostgreSQL | bounded-context schemas and migrations |
| Cache and rate limits | Redis | ephemeral cache only |
| Events | RabbitMQ | outbox publisher and consumer tests |
| Documents | MinIO | private S3-compatible object storage |
| Authorization server | Keycloak | OIDC, JWKS, client, role, and session tests |
| Identity provider | Mock Fayda adapter | deterministic verification success/failure/replay cases |
| Payments | Mock payment adapter | pending/success/failure/webhook/reversal cases |

Required local conventions:

- `.env.example` contains only non-secret defaults and endpoint names.
- Real credentials are supplied through ignored local configuration or environment variables.
- Seed data is synthetic and clearly named as test data.
- Provider callbacks use local HTTPS or an explicit test-only exception with no production reuse.
- Tests can run with mocks and do not call Fayda, payment, SMS, or storage production endpoints.
- The application starts with one command after dependencies are healthy and exposes health/readiness endpoints.
- Database migrations are identical in shape to staging and production migrations.
