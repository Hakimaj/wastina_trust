# ADR-019: Occupation classification is reference-data, not a forced legal identity

- Status: Accepted
- Date: 2026-09-30

## Decision

The platform supports bilingual job titles and optional standardized occupation codes while preserving local Ethiopian terminology.

## Context

Not every role in Ethiopia maps cleanly to an external classification taxonomy. A title may be locally meaningful even when a standardized code is unavailable or partial.

## Consequences

- local labor terminology remains usable
- ISCO-like classification can be attached when appropriate
- historical records remain interpretable across classification versions
- classification differences are explicit instead of silently normalized

## Follow-ups

- add import process for occupation reference data
- track classification version for historical interpretation
