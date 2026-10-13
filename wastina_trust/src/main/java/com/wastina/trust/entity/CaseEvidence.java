package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_evidence", indexes = {
        @Index(name = "idx_case_evidence_case", columnList = "case_id"),
        @Index(name = "idx_case_evidence_document", columnList = "document_reference")
})
@Data
@NoArgsConstructor
public class CaseEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private ComplianceCase complianceCase;

    @Column(name = "document_reference", nullable = false, length = 200)
    private String documentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 40)
    private EvidenceType type;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "integrity_algorithm", length = 30)
    private String integrityAlgorithm;

    @Column(name = "integrity_hash", length = 128)
    private String integrityHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvidenceStatus status = EvidenceStatus.SCAN_PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvidenceConfidentiality confidentiality = EvidenceConfidentiality.CASE_INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Version
    private Long version;

    public void markAvailable() {
        if (status != EvidenceStatus.SCAN_PENDING && status != EvidenceStatus.UPLOADED) {
            throw new IllegalStateException("Only pending evidence can become available.");
        }
        status = EvidenceStatus.AVAILABLE;
    }

    public void reject() {
        if (status == EvidenceStatus.REJECTED || status == EvidenceStatus.WITHDRAWN) {
            throw new IllegalStateException("Evidence is already unavailable.");
        }
        status = EvidenceStatus.REJECTED;
    }
}
