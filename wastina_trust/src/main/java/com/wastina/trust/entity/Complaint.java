package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaints", uniqueConstraints = @UniqueConstraint(name = "uk_complaint_public_reference", columnNames = "public_reference"), indexes = {
        @Index(name = "idx_complaint_subject", columnList = "subject_user_id"),
        @Index(name = "idx_complaint_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_reference", nullable = false, unique = true, length = 40)
    private String publicReference = "WST-CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complainant_user_id", nullable = false)
    private User complainant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_user_id", nullable = false)
    private User subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ComplaintCategory category;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "relationship_type", nullable = false, length = 40)
    private String relationshipType;

    @Column(name = "source_entity_type", nullable = false, length = 60)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false, length = 100)
    private String sourceEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseSeverity severity;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private Long version;

    public void submit() {
        if (status != ComplaintStatus.DRAFT) {
            throw new IllegalStateException("Only a draft complaint can be submitted.");
        }
        if (description == null || description.trim().length() < 20) {
            throw new IllegalArgumentException("Complaint description must contain meaningful detail.");
        }
        status = ComplaintStatus.SUBMITTED;
        submittedAt = Instant.now();
        updatedAt = submittedAt;
    }

    public boolean isFindingEstablished() {
        return false;
    }
}
