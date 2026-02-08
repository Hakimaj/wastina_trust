package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "employer_profiles")
@Data
@NoArgsConstructor
public class EmployerProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String companyName;

    // Address is in User.location

    private String subscriptionTier; // FREE, PREMIUM

    private Integer totalHires = 0;

    public EmployerProfile(User user) {
        this.user = user;
    }
}
