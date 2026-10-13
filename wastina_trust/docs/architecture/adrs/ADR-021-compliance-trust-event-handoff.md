# ADR-021: Compliance-to-Trust handoff uses canonical outcome events

- Status: Accepted for Task 6
- Date: 2026-09-30

## Decision

Compliance emits minimal canonical outcome events after committed state. Trust decides eligibility, weighting, recency, confidence, reversal, and score calculation.

## Reason

A complaint must not reduce Trust Score merely because it was filed. A final platform outcome may be relevant to Trust policy, while an appeal or reversal must remain expressible as a corrective event.

## Consequences

- no `score_delta` is emitted by Compliance
- event IDs and versions support deduplication and replay
- complaint and evidence narratives stay out of ordinary Trust events
- event publication must use a transactional outbox before production
- Trust outages do not roll back an already committed compliance decision
