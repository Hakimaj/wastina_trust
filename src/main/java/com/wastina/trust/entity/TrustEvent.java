package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_events", uniqueConstraints = @UniqueConstraint(name = "uk_trust_source_event", columnNames = {"source_service", "source_event_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TrustEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    @Column(name = "source_event_id", nullable = false, length = 100)
    private String sourceEventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 60)
    private String sourceService;

    @Column(nullable = false)
    private UUID subjectUserId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant effectiveAt;

    @Column(nullable = false, precision = 5, scale = 4)
    private java.math.BigDecimal quality;

    @Column(nullable = false)
    private boolean adjudicated;

    @Column(nullable = false)
    private boolean positive;

    @Column(nullable = false)
    private boolean negative;
}