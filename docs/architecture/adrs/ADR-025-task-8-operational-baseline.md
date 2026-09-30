# ADR-025: Task 8 operational baseline remains a modular monolith

- Status: Accepted for Task 8
- Date: 2026-09-30

## Decision

Keep the Spring Boot modular monolith and PostgreSQL migration stream for the current project stage. Add a transactional outbox persistence boundary, health/metrics endpoints, local Docker Compose dependencies, a non-root container, and CI build/security-artifact checks. Keep Kubernetes deferred.

## Evidence

The repository has one Spring Boot application, no committed service deployment manifests, no broker publisher, no production storage/scanner adapters, and a small domain test suite. Splitting into many deployables would add operational complexity without current evidence of independent scaling or team ownership needs.

## Consequences

- local and staging workflows are simpler to reproduce
- service ownership remains logical and extraction-ready
- production still requires managed database/storage/secrets, RabbitMQ publisher/consumers, audit, security tests, and DR drills
- the outbox row is durable infrastructure, not proof that broker delivery is complete

## Migration plan

1. wire outbox publisher/inbox/DLQ using the existing RabbitMQ decision
2. add contract/integration tests and schema compatibility gates
3. deploy staging on a managed container/VM target
4. revisit extraction or Kubernetes only after measured operational evidence
