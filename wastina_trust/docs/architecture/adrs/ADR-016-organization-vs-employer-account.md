# ADR-016: Organization vs employer account separation

- Status: Accepted
- Date: 2026-09-30

## Decision

The platform separates the legal/business organization from the Wastina user account and from person-level roles.

## Context

A user may be an employee, a former employee, an HR representative, and a manager across different organizations. The same person must not automatically be treated as an employer or verifier for all organizations.

## Consequences

- organization authority is scoped to a business entity
- verification power is granted only through active membership and permissions
- a user may have multiple historical and current organization relationships without conflation
- audit trails stay attributable to the specific actor and the specific organization context

## Follow-ups

- confirm onboarding, verification, and suspension flows for organizations
- require explicit organization membership permission before any verification action
