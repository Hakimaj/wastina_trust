package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "guarantees",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "public_reference")
    }
)
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Guarantee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_reference", nullable = false, unique = true)
    private String publicReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuaranteeType type = GuaranteeType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GuaranteeStatus status = GuaranteeStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_id", nullable = false)
    private User principal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id", nullable = false)
    private User guarantor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private User beneficiary;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 6)
    private String currency = "ETB";

    @Column(precision = 18, scale = 2)
    private BigDecimal guaranteedAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal outstandingLiability;

    @Column(length = 500)
    private String purpose;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "acceptance_deadline")
    private Instant acceptanceDeadline;

    @Column(name = "terms_version")
    private String termsVersion = "v1";

    @Column(name = "contract_version")
    private String contractVersion = "v1";

    @Column(name = "contract_hash")
    private String contractHash;

    @Column(name = "signature_status")
    private String signatureStatus = "NOT_REQUIRED";

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public void accept() {
        if (this.status == GuaranteeStatus.PENDING_ACCEPTANCE || this.status == GuaranteeStatus.DRAFT) {
            this.status = GuaranteeStatus.ACCEPTED;
            this.acceptedAt = Instant.now();
        }
    }

    public void activate() {
        if (this.status == GuaranteeStatus.ACCEPTED) {
            this.status = GuaranteeStatus.ACTIVE;
        }
    }

    public void complete() {
        if (this.status == GuaranteeStatus.ACTIVE || this.status == GuaranteeStatus.CLAIMED) {
            this.status = GuaranteeStatus.FULFILLED;
        }
    }
}
