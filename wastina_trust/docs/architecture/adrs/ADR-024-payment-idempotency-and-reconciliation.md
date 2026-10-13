# ADR-024: Payment idempotency, verification, and reconciliation

- Status: Accepted for Task 7
- Date: 2026-09-30

## Decision

Payment intent idempotency, provider transaction identity, webhook identity, server-side verification, and reconciliation are separate controls. Unknown provider outcomes remain unknown until established. Amount/currency/reference mismatches require reconciliation and cannot activate downstream business state.

## Consequences

- duplicate callbacks have one business effect
- a payment intent is not the same as a provider transaction
- provider-specific status and signature behavior stays in adapters
- refunds are separate bounded records with amount limits and idempotency
- Guarantee consumes canonical payment events and decides its own state transition
- provider contracts and regulatory payment responsibilities remain human review items
