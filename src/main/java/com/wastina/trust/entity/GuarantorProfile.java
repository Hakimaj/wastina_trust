package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "guarantor_profiles")
@Data
@NoArgsConstructor
public class GuarantorProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal financialLimit;

    private String employerName; // Where the guarantor works

    private String kycStatus; // NONE, PENDING, VERIFIED

    public GuarantorProfile(User user) {
        this.user = user;
    }
}
