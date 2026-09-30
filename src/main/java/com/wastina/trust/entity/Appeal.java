package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_appeals", indexes = @Index(name = "idx_appeal_case", columnList = "case_id"))
@Data
@NoArgsConstructor
public class Appeal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private ComplianceCase complianceCase;

    @Column(name = "appellant_user_id", nullable = false)
    private UUID appellantUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppealGrounds grounds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppealStatus status = AppealStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", length = 20)
    private AppealDecisionType decisionType;

    @Column(length = 4000)
    private String explanation;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Appeal(ComplianceCase complianceCase, UUID appellantUserId, AppealGrounds grounds) {
        this.complianceCase = complianceCase;
        this.appellantUserId = appellantUserId;
        this.grounds = grounds;
    }

    public void submit() {
        if (status != AppealStatus.SUBMITTED) {
            throw new IllegalStateException("Appeal is not in a submittable state.");
        }
        submittedAt = Instant.now();
    }

    public void beginReview() {
        if (status != AppealStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted appeals can enter review.");
        }
        status = AppealStatus.IN_REVIEW;
    }

    public void resolve(AppealDecisionType decisionType) {
        if (status != AppealStatus.IN_REVIEW) {
            throw new IllegalStateException("Only appeals under review can be resolved.");
        }
        this.decisionType = decisionType;
        this.status = AppealStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}
