package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "worker_profiles")
@Data
@NoArgsConstructor
public class WorkerProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "profession_id")
    private Profession profession;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    private Integer trustScore = 0;

    private boolean isAvailable = true;

    private boolean verificationBadge = false;

    public WorkerProfile(User user) {
        this.user = user;
    }
}
