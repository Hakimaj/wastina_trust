package com.wastina.trust.entity;

import com.wastina.trust.trust.TrustConfidence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trust_score_history")
@Getter
@Setter
@NoArgsConstructor
public class TrustScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int previousScore;

    @Column(nullable = false)
    private int newScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrustConfidence previousConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrustConfidence newConfidence;

    @Column(nullable = false, length = 30)
    private String policyVersion;

    @Column(nullable = false, length = 60)
    private String reasonCode;

    private UUID triggerEventId;

    @Column(nullable = false)
    private Instant createdAt;
}