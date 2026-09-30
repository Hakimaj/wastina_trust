# ADR-022: Separate Financial and Document Gateways

- Status: Accepted for Task 7
- Date: 2026-09-30

## Decision

Payment and Document are independent infrastructure bounded contexts. They may share authentication, authorization, secrets, observability, audit, and event infrastructure, but they do not share domain state or provider/storage responsibilities.

## Consequences

- Guarantee requests payment operations and decides guarantee transitions; Payment does not activate guarantees.
- Compliance and Employment reference document IDs; Document owns storage, scanning, versions, and secure access.
- Provider and storage adapters remain behind ports and are replaceable.
- Payment transactions, callbacks, reconciliation, and refunds are separate from payment intent state.
- Document metadata is relational; binaries are private object-storage artifacts.
