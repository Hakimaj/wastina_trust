package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_ratings", indexes = {
        @Index(name = "idx_rating_record", columnList = "employment_record_id"),
        @Index(name = "idx_rating_rater", columnList = "rater_user_id")
})
@Data
@NoArgsConstructor
public class EmploymentRating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_record_id", nullable = false)
    private EmploymentRecord employmentRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_user_id", nullable = false)
    private User raterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentRatingStatus status = EmploymentRatingStatus.SUBMITTED;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "visibility")
    private String visibility = "restricted";

    @Column(name = "review_state")
    private String reviewState = "PENDING";

    public EmploymentRating(EmploymentRecord employmentRecord, User raterUser, int rating) {
        this.employmentRecord = employmentRecord;
        this.raterUser = raterUser;
        this.rating = rating;
        validateRating();
    }

    @PrePersist
    @PreUpdate
    public void validateRating() {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Employment rating must be between 1 and 5.");
        }
    }
}
