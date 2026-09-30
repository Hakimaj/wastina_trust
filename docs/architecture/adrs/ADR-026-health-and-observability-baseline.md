# ADR-026: Health and observability baseline

- Status: Accepted for Task 8
- Date: 2026-09-30

## Decision

Use Spring Boot Actuator for safe liveness/readiness/metrics exposure, with detailed health protected by authorization. Keep application logs, security logs, immutable audit records, and event payloads conceptually separate. Add OpenTelemetry-compatible tracing when the deployment backend is selected.

## Consequences

- health probes can be used by a future managed container platform
- operational metrics are available without exposing domain data
- dashboards and alerts remain deployment-specific follow-up work
- no claim of production observability is made until metrics backend, trace propagation, alert ownership, and runbooks are exercised
