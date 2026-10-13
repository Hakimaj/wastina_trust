package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_evidence")
@Data
@NoArgsConstructor
public class EmploymentEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_record_id", nullable = false)
    private EmploymentRecord employmentRecord;

    @Column(name = "document_reference", nullable = false)
    private String documentReference;

    @Column(name = "document_hash")
    private String documentHash;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "verification_relationship")
    private String verificationRelationship;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "review_status")
    private String reviewStatus = "PENDING";

    public EmploymentEvidence(EmploymentRecord employmentRecord, String documentReference, String purpose) {
        this.employmentRecord = employmentRecord;
        this.documentReference = documentReference;
        this.purpose = purpose;
    }
}
