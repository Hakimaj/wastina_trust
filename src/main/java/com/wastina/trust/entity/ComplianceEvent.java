package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_events", uniqueConstraints = @UniqueConstraint(name = "uk_compliance_event_id", columnNames = "event_id"), indexes = @Index(name = "idx_compliance_event_case", columnList = "case_id"))
@Data
@NoArgsConstructor
public class ComplianceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ComplianceEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;

    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "source_service", nullable = false, length = 60)
    private String sourceService = "compliance-service";

    @Column(name = "event_version", nullable = false)
    private int eventVersion = 1;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private boolean adjudicated;

    public static ComplianceEvent outcome(ComplianceEventType type, ComplianceCase complianceCase, UUID subjectUserId) {
        ComplianceEvent event = new ComplianceEvent();
        event.eventType = type;
        event.complianceCase = complianceCase;
        event.subjectUserId = subjectUserId;
        event.adjudicated = type == ComplianceEventType.CASE_UPHELD
                || type == ComplianceEventType.CASE_PARTIALLY_UPHELD
                || type == ComplianceEventType.CASE_OVERTURNED;
        return event;
    }
}
