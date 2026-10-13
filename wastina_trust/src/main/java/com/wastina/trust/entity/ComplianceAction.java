package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_actions", indexes = @Index(name = "idx_action_case", columnList = "case_id"))
@Data
@NoArgsConstructor
public class ComplianceAction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private ComplianceCase complianceCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ComplianceActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceActionStatus status = ComplianceActionStatus.ACTIVE;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "applied_by", nullable = false)
    private UUID appliedBy;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "review_at")
    private Instant reviewAt;

    public void activate() {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A temporary compliance action requires a reason.");
        }
        if (expiresAt == null && reviewAt == null) {
            throw new IllegalArgumentException("A temporary compliance action requires an expiry or review date.");
        }
        if (expiresAt != null && startsAt != null && expiresAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Action expiry cannot precede its start.");
        }
        status = ComplianceActionStatus.ACTIVE;
        startsAt = startsAt == null ? Instant.now() : startsAt;
    }

    public void release() {
        if (status != ComplianceActionStatus.ACTIVE) {
            throw new IllegalStateException("Only active actions can be released.");
        }
        status = ComplianceActionStatus.RELEASED;
    }
}
