package com.wastina.trust.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_consents")
@Getter
@Setter
@NoArgsConstructor
public class Consent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String purpose;

    @Column(nullable = false, length = 100)
    private String policyVersion;

    @Column(nullable = false)
    private Instant grantedAt;

    private Instant withdrawnAt;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(nullable = false, length = 20)
    private String locale;
}