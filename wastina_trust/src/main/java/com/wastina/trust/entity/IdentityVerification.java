package com.wastina.trust.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_verifications")
@Getter
@Setter
@NoArgsConstructor
public class IdentityVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, unique = true, length = 200)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IdentityStatus identityStatus;

    @Column(length = 50)
    private String assuranceLevel;

    @Column(length = 80)
    private String failureReasonCode;

    @Column(nullable = false)
    private UUID consentId;

    @Column(nullable = false)
    private Instant initiatedAt;

    private Instant completedAt;

    @Version
    private long version;
}