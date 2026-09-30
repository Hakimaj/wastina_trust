package com.wastina.trust.entity;

import com.wastina.trust.trust.TrustConfidence;
import com.wastina.trust.trust.TrustEvidenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_scores")
@Getter
@Setter
@NoArgsConstructor
public class TrustScoreSnapshot {
    @Id
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal internalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrustConfidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrustEvidenceStatus evidenceStatus;

    @Column(nullable = false, length = 30)
    private String policyVersion;

    @Column(nullable = false)
    private Instant calculatedAt;

    @Version
    private long version;
}