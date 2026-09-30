# Task 3: Trust and Reputation Engine

**Author:** Hakimaj (Lakim)
**Status:** Deterministic MVP foundation implemented; governance and provider integrations require review
**Date:** 2026-09-30

## 1. Repository Audit and Architecture Compliance

The repository is a Spring Boot modular monolith under `src/main/java/com/wastina/trust`. Task 1 froze PostgreSQL as the system of record, OIDC/IAM as the identity boundary, transactional outbox as the durable event pattern, and separate bounded-context ownership. Task 2 established `User.identitySubject`, account/identity status, and the rule that Trust must not access passwords, MFA secrets, refresh tokens, raw national IDs, or provider credentials.

The previous `TrustEngineService` used a non-versioned Neo4j points counter based on Fayda presence, employment years, and a government-employed guarantor. That path was not deterministic/replayable in the required sense and treated sensitive or weak proxies as score inputs. The new score path uses a pure calculator plus a PostgreSQL Trust ledger, snapshot, and history. The existing Neo4j graph class remains only for compatibility with legacy graph synchronization and is not the authoritative score source.

## 2. Product Definition

The Trust Score is a **0-100 platform-specific indicator derived from verified historical evidence about reliability within Wastina-supported activities**. It is not a universal character judgment, credit score, legal finding, employment decision, housing decision, or guarantee of future behavior.

Trust exposes three separate concepts:

- **Score:** bounded numerical indicator.
- **Confidence:** LOW, MEDIUM, or HIGH evidence support.
- **Evidence status:** NO_DATA, LIMITED_DATA, PARTIALLY_VERIFIED, VERIFIED, UNDER_REVIEW, or DISPUTED.

Cold start is neutral: no evidence produces score `0`, confidence `LOW`, and `NO_DATA`. Identity verification alone contributes only the verification factor and does not make a user trustworthy or high-confidence.

### Separate concepts

`Trust Score` measures historical platform evidence. `Current Exposure` belongs to Guarantee/Payment and measures active obligations. `Risk Flags` represent suspicious patterns requiring review. They must not silently become score deductions.

### HUMAN DECISION REQUIRED

Product, legal, and governance owners must approve intended uses, public visibility, score bands, downstream decisions, guarantee outcome significance, employment contribution, ratings, dispute treatment, retention, appeal rights, and which workflows require identity verification. The score must never be the sole automatic reason for housing, employment, financial, legal, or government eligibility decisions without separate approved policy.

## 3. Mathematical Specification

### Active candidate policy `1.0`

The implementation exposes an immutable candidate policy:

| Factor | Weight | Meaning |
|---|---:|---|
| Employment reliability | 0.25 | Verified employment outcomes, not income or social status |
| Guarantee reliability | 0.30 | Canonical verified guarantee outcomes |
| Verified feedback | 0.15 | Verified feedback facts, with future sample-size/shrinkage governance |
| Dispute record | 0.20 | Adjudicated outcomes only; pending complaints do not deduct |
| Verification quality | 0.10 | Identity assurance status, never raw identity data |

These are **illustrative candidate values, not scientifically validated production weights**. Policy weights must sum to `1.00`, be validated before activation, be immutable after activation, and require maker-checker approval, simulation, security review, privacy/legal review, business approval, effective date, and audit.

### Factor calculation

For each eligible event $e$ in factor $f$:

$$
q_e = quality(e) \in [0,1]
$$

$$
Dir_e = \begin{cases}
-1 & \text{if the adjudicated event is negative} \\
+1 & \text{if the verified event is positive} \\
0 & \text{otherwise}
\end{cases}
$$

The current deterministic implementation applies a transparent linear recency approximation:

$$
R_e = max\left(0, 1 - 0.08 \cdot \frac{ageDays(e)}{365}\right)
$$

and computes:

$$
Raw_f = \frac{1}{N_f} \sum_{e \in f} Dir_e \cdot (100 \cdot q_e) \cdot R_e
$$

$$
Normalized_f = clamp\left(\frac{Raw_f + 100}{2}, 0, 100\right)
$$

$$
Contribution_f = Weight_f \cdot Normalized_f
$$

$$
InternalScore = clamp\left(\sum_f Contribution_f, 0, 100\right)
$$

The public score is deterministic decimal arithmetic using `BigDecimal`, rounded to the nearest integer with `HALF_UP`. The calculation output also retains the internal decimal and factor trace.

The recency formula is a governance candidate and must be validated by backtesting/sensitivity analysis before production activation. Historical facts are never deleted because their relevance decays, not their truth.

### Confidence

Confidence is independent of score. Current candidate bands are evidence-count based: fewer than 3 evidence items is LOW, 3-7 is MEDIUM, 8 or more is HIGH. Governance should later add evidence diversity, source reliability, recency, sample size, and consistency. A score of 78/LOW is not equivalent to 78/HIGH.

### Worked illustrative example

Using clearly illustrative factor values after normalization:

```text
Employment Reliability = 78
Guarantee Reliability = 86
Verified Feedback = 74
Dispute Record = 90
Verification Quality = 95
Weights = 0.25, 0.30, 0.15, 0.20, 0.10
```

$$
0.25(78) + 0.30(86) + 0.15(74) + 0.20(90) + 0.10(95) = 84.20
$$

The displayed score is `84`, subject to factor caps, event eligibility, recency, confidence, policy version, and governance. The implementation stores the factor contributions and policy version rather than only the paragraph explanation.

## 4. Evidence and Event Model

Trust owns `trust_events`; source modules remain owners of employment, guarantee, dispute, payment, and identity records. A Trust event contains:

- `event_id`
- `source_event_id`
- `event_type`
- `source_service`
- `subject_user_id`
- `occurred_at`
- `effective_at`
- `quality`
- `adjudicated`
- `positive`
- `negative`

Events are accepted only from authenticated/authorized internal producers in the eventual outbox consumer. End users cannot post score-affecting events. Event identity is unique on `(source_service, source_event_id)`, so duplicate delivery has one business effect.

Canonical event examples:

- `IDENTITY_VERIFIED` from IAM
- `EMPLOYMENT_VERIFIED` and `EMPLOYMENT_ENDED` from Employment
- `GUARANTEE_COMPLETED` and `GUARANTEE_DEFAULT_CONFIRMED` from Guarantee
- `DISPUTE_UPHELD`, `DISPUTE_CLEARED`, and `DISPUTE_OVERTURNED` from Dispute
- `VERIFIED_FEEDBACK` from the authorized feedback workflow

Technical events are not independent score points when they describe one outcome. For example, payment completed, contract closed, and guarantee completed must resolve to one canonical guarantee outcome.

Delivery is at-least-once. The business guarantee is exactly-once effect through authenticated producers, unique event identity, a processed-event/unique constraint, atomic ledger/snapshot/history transaction, and retries. The system does not claim exactly-once network delivery.

### Complaints and appeals

`COMPLAINT_FILED` creates `UNDER_REVIEW` status and no permanent negative score effect. Only an authorized adjudicated `DISPUTE_UPHELD` event may affect the dispute factor. An appeal reversal creates a new `DISPUTE_OVERTURNED` compensating event and recalculation; the original decision and score history remain.

Example:

```text
Complaint filed       -> status UNDER_REVIEW; score unchanged
Decision upheld       -> DISPUTE_UPHELD; new calculation/history entry
Appeal overturned     -> DISPUTE_OVERTURNED; corrective calculation/history entry
```

## 5. Persistence and Ownership

| Table | Owner | Policy |
|---|---|---|
| `trust_events` | Trust | append-only; unique source event; source reference and minimal facts |
| `trust_scores` | Trust | derived current snapshot; bounded 0-100; optimistic version |
| `trust_score_history` | Trust | append-only; previous/new score, confidence, policy, reason, trigger |
| future `trust_calculations` | Trust | immutable calculation metadata/trace |
| future `trust_calculation_factors` | Trust | immutable structured factor trace |
| future `trust_policies` / versions | Trust governance | immutable activated version |
| future `trust_risk_flags` | Trust | review state, not automatic score penalty |
| future `trust_appeals` | Compliance/Trust boundary | appeal and decision references; no history rewrite |
| future `trust_recalculation_jobs` | Trust operations | batch progress, retry, policy version |

PostgreSQL is authoritative. Redis, if introduced later, is only a cache. On cache failure, read PostgreSQL. A score update must commit snapshot/history/outbox atomically; cache update follows commit and can be retried.

The current migration adds `trust_events`, `trust_scores`, and `trust_score_history`, with score bounds and source-event uniqueness. Detailed policy tables, calculation traces, risk flags, appeals, jobs, and outbox integration are follow-up slices and are not represented as complete production features yet.

## 6. Deterministic Calculation Pipeline

```text
Canonical authenticated events
  -> eligibility validation
  -> source-event deduplication
  -> evidence quality/adjudication filtering
  -> as-of timestamp filtering
  -> recency adjustment
  -> factor normalization
  -> policy weights and caps
  -> score clamp and deterministic rounding
  -> confidence calculation
  -> explanation metadata
  -> snapshot/history persistence
  -> cache update after commit
  -> authorized API response
```

`DeterministicTrustCalculator.calculate(inputFacts, policy)` is pure with respect to its input facts, policy, and `asOf` time. It performs no network calls, random operations, or hidden database reads. Historical replay passes the same facts, policy version, and `asOf` timestamp.

PostgreSQL read-committed behavior permits concurrent commands to see different committed snapshots; score writes therefore require one transaction and optimistic version handling or per-user serialization. The implementation uses a versioned snapshot and must add retry/locking around concurrent event processing before production scale. PostgreSQL `SELECT FOR UPDATE` or transaction-level advisory locks are valid per-user strategies; a global lock is not acceptable.

## 7. Anti-Gaming and Fairness Model

The MVP is deterministic rules, not machine learning. Future anomaly detection must emit `RiskFlag`/`AnomalyAssessment`, not directly overwrite the score.

Required controls before production:

- verified internal event producers only;
- unique source-event identity and event schema version;
- canonical guarantee outcomes to prevent technical-event double counting;
- per-counterparty/source concentration caps;
- reciprocal rating and guarantee pattern flags;
- Sybil/duplicate identity controls delegated to IAM;
- rate limits and source quotas;
- pending/review state instead of automatic punishment from suspicion;
- maker-checker policy and manual-adjustment workflows;
- append-only history and tamper-evident monitoring.

The score must not use ethnicity, religion, gender, language preference, political affiliation, nationality, disability, wealth, salary, or similar protected/sensitive characteristics. Proxy risks such as location, occupation, employer type, education, and device traits require offline review before use. Removing explicit protected attributes does not prove fairness.

Fairness evaluation follows NIST AI RMF concepts: Govern, Map, Measure, Manage. Even though this MVP is rules-based rather than ML, governance must measure score distributions, missing-data disparities, volatility, calibration where a legitimate ground truth exists, appeal/overturn patterns, and downstream outcome disparities. No fairness claim is made until appropriate data, legal basis, and human review exist.

## 8. API Contract

The existing `/api/trust-engine/{workerId}/score` remains as a compatibility route but now returns score, confidence, evidence status, policy version, evidence count, and factor contribution strings. New versioned routes should be introduced before expanding clients:

```text
GET  /api/v1/trust/me
GET  /api/v1/trust/users/{userId}
GET  /api/v1/trust/users/{userId}/history
POST /api/v1/admin/trust/users/{userId}/recalculate
POST /api/v1/internal/trust/events
```

`/internal/trust/events` must require service identity, allowed event type, schema validation, source reference, effective timestamp, and idempotency. A client cannot set a score or submit a positive event. User-facing responses must hide private disputes, evidence, internal risk flags, source notes, and fraud thresholds. Object authorization is required for every user ID; an unpredictable ID is not authorization.

Errors include `TRUST_PROFILE_NOT_FOUND`, `TRUST_ACCESS_FORBIDDEN`, `DUPLICATE_EVENT`, `INVALID_EVENT`, `CALCULATION_NOT_AVAILABLE`, `POLICY_NOT_ACTIVE`, and `RECALCULATION_ALREADY_RUNNING`.

## 9. Required Test Strategy

Implemented focused tests cover:

- cold start: score 0, LOW confidence, NO_DATA;
- complaint-only input: no score penalty and UNDER_REVIEW status;
- duplicate event: same score/evidence count as one event;
- identity-only evidence: small bounded contribution, not perfect trust;
- exact decimal deterministic calculator behavior.

Required next tests:

- upheld then overturned dispute compensation;
- as-of replay and policy migration;
- out-of-order and late events;
- unauthorized service event rejection;
- IDOR/private factor access;
- concurrent same-user recalculation with optimistic retry/per-user lock;
- property: score always 0-100;
- property: display language does not change score;
- event replay produces deterministic result;
- policy validation and maker-checker approval;
- cache failure and worker crash recovery;
- synthetic collusion, reciprocity, Sybil, mixed, and duplicate datasets;
- load test retrieval, event bursts, and batch recalculation.

No production benchmark or full Maven build result is claimed. The repository's Maven clean is blocked by a locked generated `target/classes` directory; focused Trust calculator tests passed `5/5`.

## 10. Security and Threat Summary

| Threat | Control | Residual risk |
|---|---|---|
| Forged positive event | OIDC service identity, producer allow-list, schema validation | compromised producer |
| Duplicate/replay | source-event unique constraint and idempotent transaction | malformed source identity |
| Deleted/altered history | append-only DB role, audit sink, later hash chain | privileged DB compromise |
| Complaint weaponization | complaint is status only; adjudication required | process/governance failure |
| Rating/guarantee farming | concentration and reciprocity risk flags | collusion sophistication |
| Policy tampering | immutable versions, simulation, maker-checker | privileged governance compromise |
| Private score enumeration | object authorization, rate limits, bounded APIs | authorized counterparty misuse |
| Stale cache | PostgreSQL source of truth, post-commit invalidation | temporary stale read |
| Concurrent lost update | optimistic version/per-user serialization and retry | implementation defect |

## 11. Human Review Required

`HUMAN DECISION REQUIRED` for:

- meaning and intended use of the Trust Score;
- candidate weights, caps, decay, confidence thresholds, and score visibility;
- permissible data sources and dispute treatment;
- retention, deletion, privacy notice, and downstream decisions;
- policy owner, security reviewer, legal/privacy reviewer, business approver;
- manual adjustments, freezes, appeals, and severe score changes;
- producer authorization and audit-integrity controls;
- fairness datasets, proxy-variable assessment, and evaluation metrics.

## 12. Task 3 Architecture Freeze

Unless a later ADR changes them, later tasks must rely on:

1. Trust Score is a platform-specific reliability indicator, not universal human worth.
2. Score, confidence, and evidence status are separate.
3. Cold start is neutral/low-confidence, not perfect behavior.
4. Deterministic decimal rules are the MVP; ML is not the score engine.
5. PostgreSQL Trust event ledger is the replay source; source domains remain canonical owners.
6. Duplicate event effects are prevented by source identity uniqueness and idempotent processing.
7. Complaints do not deduct score; adjudicated outcomes and appeal corrections are separate events.
8. Snapshots are derived; score history is append-only and never rewritten.
9. Policy versions are immutable and must be governed, simulated, and audited.
10. Risk flags remain distinct from Trust Score.
11. Redis, if added, is only derived cache.
12. Trust cannot read IAM credentials/raw national-ID data or directly modify Employment, Guarantee, Dispute, Payment, or Identity records.
13. All future score-affecting events use authenticated service contracts and minimal payloads.
14. Later public/API exposure must implement object-level authorization and privacy-filtered explanations.

## 13. References

- NIST, [AI Risk Management Framework](https://www.nist.gov/itl/ai-risk-management-framework): voluntary framework for managing risks to individuals, organizations, and society and incorporating trustworthiness considerations.
- NIST, [AI RMF Playbook](https://airc.nist.gov/AI_RMF_Knowledge_Base/Playbook): Govern, Map, Measure, and Manage actions covering validity, reliability, transparency, explainability, privacy, fairness, human oversight, monitoring, and contestability.
- PostgreSQL, [Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html): Read Committed behavior, Serializable semantics, and retry requirements for serialization failures.
- PostgreSQL, [Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html): row-level locks, advisory locks, deadlocks, and consistent locking discipline.
- Task 1, [System architecture](task-1-system-architecture.md).
- Task 2, [IAM handoff](task-2-iam-implementation.md).

## 14. Trust Engine Handoff Contract

Guarantee may emit `GuaranteeCompleted` and `GuaranteeDefaultConfirmed`. Employment may emit `EmploymentVerified` and `EmploymentEnded`. Dispute may emit `DisputeUpheld`, `DisputeCleared`, and `DisputeOverturned`. IAM may emit `IdentityVerified` and `UserSuspended`. Trust consumes authenticated, versioned, minimal events and derives score state; it never modifies those source records.
