package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_cases", uniqueConstraints = @UniqueConstraint(name = "uk_case_public_reference", columnNames = "public_reference"), indexes = {
        @Index(name = "idx_case_subject", columnList = "subject_user_id"),
        @Index(name = "idx_case_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class ComplianceCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_reference", nullable = false, unique = true, length = 40)
    private String publicReference = "WST-CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_user_id", nullable = false)
    private User subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", length = 30)
    private DecisionType decisionType;

    @Column(name = "decision_reason", length = 4000)
    private String decisionReason;

    @Column(name = "policy_version", length = 40)
    private String policyVersion;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Version
    private Long version;

    public void open() {
        if (status != CaseStatus.OPEN) {
            throw new IllegalStateException("Case is already opened or progressed.");
        }
        openedAt = Instant.now();
    }

    public void startInvestigation() {
        requireStatus(CaseStatus.OPEN);
        status = CaseStatus.INVESTIGATION;
    }

    public void beginDecision() {
        requireStatus(CaseStatus.INVESTIGATION);
        status = CaseStatus.DECISION_PENDING;
    }

    public void decide(DecisionType decisionType) {
        requireStatus(CaseStatus.DECISION_PENDING);
        if (decisionType == null) {
            throw new IllegalArgumentException("Decision type is required.");
        }
        this.decisionType = decisionType;
        this.decidedAt = Instant.now();
        this.status = CaseStatus.DECIDED;
    }

    public void markAppealed() {
        requireStatus(CaseStatus.DECIDED);
        status = CaseStatus.APPEALED;
    }

    public void finalizeCase() {
        if (status != CaseStatus.DECIDED && status != CaseStatus.APPEALED) {
            throw new IllegalStateException("Only decided or appealed cases can be finalized.");
        }
        status = CaseStatus.FINAL;
        finalizedAt = Instant.now();
    }

    private void requireStatus(CaseStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Invalid case transition from " + status + "; expected " + expected + ".");
        }
    }
}
