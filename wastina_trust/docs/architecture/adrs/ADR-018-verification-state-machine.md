# ADR-018: Verification state machine is explicit and field-aware

- Status: Accepted
- Date: 2026-09-30

## Decision

Employment verification uses explicit states and supports field-level verification rather than a single `verified=true/false` flag.

## Context

A worker's claim may be partially correct: the title and dates may be right while the end date is disputed.

## Consequences

- partial verification is supported and preserved
- the employer can reject or dispute only selected fields or the whole record
- the system can explain exactly what was verified and what remains disputed
- trust events remain precise and auditable

## Follow-ups

- add full field-level assertion persistence for title, dates, department, and type
- add correction and appeal workflows
