package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_verification_requests", indexes = {
        @Index(name = "idx_verification_request_record", columnList = "employment_record_id"),
        @Index(name = "idx_verification_request_org", columnList = "target_organization_id"),
        @Index(name = "idx_verification_request_status", columnList = "status")
})
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmploymentVerificationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_record_id", nullable = false)
    private EmploymentRecord employmentRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requesterUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_organization_id", nullable = false)
    private Organization targetOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_verifier_user_id")
    private User targetVerifierUser;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentVerificationRequestStatus status = EmploymentVerificationRequestStatus.CREATED;

    @Column(name = "response_at")
    private LocalDateTime responseAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 30)
    private VerificationMethod verificationMethod = VerificationMethod.EMPLOYER_PORTAL;

    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "token_reference")
    private String tokenReference;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void create() {
        this.status = EmploymentVerificationRequestStatus.CREATED;
    }

    public void markSent() {
        transitionTo(EmploymentVerificationRequestStatus.SENT, EmploymentVerificationRequestStatus.CREATED);
    }

    public void markDelivered() {
        transitionTo(EmploymentVerificationRequestStatus.DELIVERED, EmploymentVerificationRequestStatus.SENT);
    }

    public void markViewed() {
        transitionTo(EmploymentVerificationRequestStatus.VIEWED, EmploymentVerificationRequestStatus.DELIVERED, EmploymentVerificationRequestStatus.SENT);
    }

    public void markInReview() {
        transitionTo(EmploymentVerificationRequestStatus.IN_REVIEW, EmploymentVerificationRequestStatus.CREATED,
            EmploymentVerificationRequestStatus.SENT, EmploymentVerificationRequestStatus.VIEWED,
            EmploymentVerificationRequestStatus.DELIVERED);
    }

    public void markVerified() {
        transitionTo(EmploymentVerificationRequestStatus.VERIFIED, EmploymentVerificationRequestStatus.IN_REVIEW, EmploymentVerificationRequestStatus.VIEWED);
    }

    public void markPartiallyVerified() {
        transitionTo(EmploymentVerificationRequestStatus.PARTIALLY_VERIFIED, EmploymentVerificationRequestStatus.IN_REVIEW, EmploymentVerificationRequestStatus.VIEWED);
    }

    public void markDisputed() {
        transitionTo(EmploymentVerificationRequestStatus.DISPUTED, EmploymentVerificationRequestStatus.IN_REVIEW, EmploymentVerificationRequestStatus.VIEWED);
    }

    public void markRejected() {
        transitionTo(EmploymentVerificationRequestStatus.REJECTED, EmploymentVerificationRequestStatus.IN_REVIEW, EmploymentVerificationRequestStatus.VIEWED);
    }

    public void markExpired() {
        transitionTo(EmploymentVerificationRequestStatus.EXPIRED, EmploymentVerificationRequestStatus.CREATED, EmploymentVerificationRequestStatus.SENT, EmploymentVerificationRequestStatus.DELIVERED, EmploymentVerificationRequestStatus.VIEWED, EmploymentVerificationRequestStatus.IN_REVIEW);
    }

    public void markCancelled() {
        transitionTo(EmploymentVerificationRequestStatus.CANCELLED, EmploymentVerificationRequestStatus.CREATED, EmploymentVerificationRequestStatus.SENT, EmploymentVerificationRequestStatus.DELIVERED);
    }

    public void markRevoked() {
        transitionTo(EmploymentVerificationRequestStatus.REVOKED, EmploymentVerificationRequestStatus.VERIFIED, EmploymentVerificationRequestStatus.PARTIALLY_VERIFIED, EmploymentVerificationRequestStatus.DISPUTED, EmploymentVerificationRequestStatus.REJECTED);
    }

    private void transitionTo(EmploymentVerificationRequestStatus newStatus, EmploymentVerificationRequestStatus... allowed) {
        for (EmploymentVerificationRequestStatus allowedState : allowed) {
            if (this.status == allowedState) {
                this.status = newStatus;
                this.responseAt = LocalDateTime.now();
                return;
            }
        }
        throw new com.wastina.trust.controller.BusinessRuleViolationException(
                    com.wastina.trust.controller.ApiErrorCodes.INVALID_STATE_TRANSITION,
                    "Invalid state transition from " + this.status + " to " + newStatus + ".");
    }
}
