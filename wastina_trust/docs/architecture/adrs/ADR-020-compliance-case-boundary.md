# ADR-020: Compliance case boundary and controlled outcomes

- Status: Accepted for Task 6
- Date: 2026-09-30

## Decision

Compliance is a separate bounded context owning complaints, cases, investigations, evidence references, decisions, appeals, temporary actions, and canonical compliance events. Complaint intake, investigation, decision, and appeal are separate records and state machines.

## Reason

A complaint is an allegation, not proof. A platform decision must remain distinguishable from a court, tribunal, regulator, or labour-authority determination. Separate records preserve due process, confidentiality, correction, reversal, and auditability.

## Consequences

- the Trust Service receives eligible outcome facts, never complaint-driven score deltas
- Document Service owns binary evidence and scanning
- IAM remains the identity and permission authority
- final decisions are append-only in effect; reversals create corrective history
- high-impact roles require separation of duties and case scope

## Human review required

Complaint eligibility, evidence standards, notification, retention, appeal rights, external referrals, user-facing terminology, and temporary restrictions require legal/compliance approval before production.
