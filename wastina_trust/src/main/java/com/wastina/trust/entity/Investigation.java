package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigations", indexes = @Index(name = "idx_investigation_case", columnList = "case_id"))
@Data
@NoArgsConstructor
public class Investigation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private ComplianceCase complianceCase;

    @Column(name = "investigator_user_id", nullable = false)
    private UUID investigatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestigationStatus status = InvestigationStatus.ASSIGNED;

    @Column(length = 4000)
    private String methodology;

    @Column(length = 4000)
    private String findings;

    @Column(name = "assigned_at")
    private Instant assignedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void start() {
        if (status != InvestigationStatus.ASSIGNED) {
            throw new IllegalStateException("Only assigned investigations can start.");
        }
        status = InvestigationStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    public void complete(String findings) {
        if (status != InvestigationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only active investigations can complete.");
        }
        if (findings == null || findings.isBlank()) {
            throw new IllegalArgumentException("Investigation findings are required.");
        }
        this.findings = findings;
        status = InvestigationStatus.COMPLETED;
        completedAt = Instant.now();
    }
}
