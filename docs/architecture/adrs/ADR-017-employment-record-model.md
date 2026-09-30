# ADR-017: Employment record model preserves claim + verification + resolution

- Status: Accepted
- Date: 2026-09-30

## Decision

The system keeps a historical employment record model that distinguishes a self-reported claim from an employer-verified assertion and from the final resolved record state.

## Context

Employment history is potentially contested and legally sensitive. A raw overwrite would erase prior truth and make dispute analysis impossible.

## Consequences

- original claims remain auditable
- verification assertions remain attributable to the employer
- corrections and revocations are explicit lifecycle events
- the Trust Engine sees authoritatively produced facts, not silently rewritten state

## Follow-ups

- add dispute-case integration and admin review flow in a later task
- maintain immutable historical evidence references
