# Wastina Operations Baseline

## Local start

1. Start dependencies with `docker compose up -d`.
2. Configure local non-secret environment variables from `src/main/resources/application.properties` overrides.
3. Run `mvn test`.
4. Run `mvn spring-boot:run` after PostgreSQL is healthy.
5. Check `/actuator/health` and `/actuator/health/readiness`.

Local credentials in Compose are synthetic and must never be reused outside development.

## Deployment

- Build: `mvn -DskipTests package`
- Container: `docker build -t wastina:<version> .`
- Deploy only an immutable image digest through an approved staging/production environment.
- Apply Flyway migrations before routing traffic.
- Verify readiness, database connectivity, event backlog, storage, scanner, provider configuration, and authentication.

## Rollback

Prefer deploying the previous compatible image. Database migrations use expand-contract compatibility; do not blindly roll back application code across destructive schema changes. For a financial or data-integrity incident, stop affected workflows, preserve evidence, reconcile external state, and use a forward fix or approved restore.

## Incident priorities

- SEV-1: payment integrity issue, sensitive data exposure, authentication compromise, or broad outage.
- SEV-2: major provider/storage/broker outage or material workflow degradation.
- SEV-3: isolated service degradation, queue backlog, or non-critical feature failure.
- SEV-4: documentation, dashboard, or low-impact defect.

## Runbooks

### Broker/outbox outage
Preserve domain state, inspect pending/dead-lettered events, stop destructive retries, restore broker connectivity, replay idempotently, and verify consumer state.

### Payment provider outage
Keep transactions pending/unknown, do not blind-retry money movement, query provider status, reconcile amount/currency/reference, and notify finance operations.

### Object storage/scanner outage
Keep documents pending/quarantined, do not publish or fall back to local/public storage, restore dependency, retry, and verify hashes.

### Credential or secret compromise
Revoke/rotate the credential, suspend affected high-risk workflows, inspect access/audit/provider records, preserve forensic evidence, and complete a post-incident review.

### Database recovery
Restore encrypted backup/PITR to an isolated environment, run migrations and consistency checks, verify payment/document/event references, then promote only after application smoke tests.

## Backup and DR requirements

Production requires encrypted PostgreSQL PITR, object versioning/replication or immutable backup, separate backup access, KMS/key recovery, broker persistence, deployment artifact retention, and a documented staging recovery exercise. RPO/RTO values remain pending business approval.
