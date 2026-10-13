package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "digital_contracts")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DigitalContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id", nullable = false)
    private User guarantor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private User employer;

    private BigDecimal liabilityAmount;

    private String termsHash; // Digital signature/hash

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime signedDate;

    // Optional status field could be added if needed
    // private String status;
}
