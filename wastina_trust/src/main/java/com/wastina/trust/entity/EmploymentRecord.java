package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_records", indexes = {
        @Index(name = "idx_employment_worker", columnList = "worker_user_id"),
        @Index(name = "idx_employment_organization", columnList = "employer_organization_id"),
        @Index(name = "idx_employment_status", columnList = "verification_status")
})
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmploymentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_user_id", nullable = false)
    private User worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_organization_id", nullable = false)
    private Organization organization;

    @Column(name = "job_title_en", nullable = false)
    private String jobTitleEn;

    @Column(name = "job_title_am")
    private String jobTitleAm;

    @Column(name = "occupation_code")
    private String occupationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_classification_id")
    private OccupationClassification occupationClassification;

    @Column
    private String department;

    @Column
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_relationship_type", nullable = false, length = 30)
    private WorkRelationshipType workRelationshipType = WorkRelationshipType.EMPLOYEE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SectorType sector = SectorType.PRIVATE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_employment", nullable = false)
    private boolean currentEmployment = false;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 30)
    private EmploymentRecordStatus status = EmploymentRecordStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private EmploymentVerificationStatus verificationStatus = EmploymentVerificationStatus.SELF_REPORTED;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public EmploymentRecord(User worker, Organization organization, String jobTitleEn) {
        this.worker = worker;
        this.organization = organization;
        this.jobTitleEn = jobTitleEn;
    }

    public static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Employment start date is required.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Employment end date cannot be before the start date.");
        }
    }

    @PrePersist
    @PreUpdate
    public void validateState() {
        validateDates(this.startDate, this.endDate);
        if (this.currentEmployment && this.endDate != null) {
            throw new IllegalArgumentException("Current employment cannot have a closed end date. Use endDate = null for active employment.");
        }
    }
}
