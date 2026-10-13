package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_verification_assertions")
@Data
@NoArgsConstructor
public class EmploymentVerificationAssertion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private EmploymentVerificationRequest request;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "original_value")
    private String originalValue;

    @Column(name = "asserted_value")
    private String assertedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentVerificationStatus status = EmploymentVerificationStatus.VERIFIED;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verifier_user_id")
    private UUID verifierUserId;

    @Column(name = "organization_id")
    private UUID organizationId;

    public EmploymentVerificationAssertion(EmploymentVerificationRequest request, String fieldName, String assertedValue,
                                          EmploymentVerificationStatus status) {
        this.request = request;
        this.fieldName = fieldName;
        this.assertedValue = assertedValue;
        this.status = status;
        this.verifiedAt = LocalDateTime.now();
    }
}
