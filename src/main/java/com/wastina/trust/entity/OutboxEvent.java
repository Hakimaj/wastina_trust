package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id"), indexes = {
        @Index(name = "idx_outbox_pending", columnList = "status, next_attempt_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Data
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId = UUID.randomUUID();

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion = 1;

    @Column(name = "source_service", nullable = false, length = 80)
    private String sourceService;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private Long version;

    public void markPublished() {
        if (status == OutboxEventStatus.PUBLISHED) {
            return;
        }
        status = OutboxEventStatus.PUBLISHED;
        publishedAt = Instant.now();
    }

    public void recordFailure(String error, int maxAttempts) {
        attemptCount++;
        lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 1000));
        status = attemptCount >= maxAttempts ? OutboxEventStatus.DEAD_LETTERED : OutboxEventStatus.FAILED;
        nextAttemptAt = Instant.now().plusSeconds(Math.min(3600, 1L << Math.min(attemptCount, 10)));
    }
}
